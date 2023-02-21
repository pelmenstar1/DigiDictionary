package io.github.pelmenstar1.digiDict.ui.addEditRecord

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.pelmenstar1.digiDict.common.*
import io.github.pelmenstar1.digiDict.common.android.viewModelAction
import io.github.pelmenstar1.digiDict.common.time.CurrentEpochSecondsProvider
import io.github.pelmenstar1.digiDict.common.time.get
import io.github.pelmenstar1.digiDict.data.*
import io.github.pelmenstar1.digiDict.widgets.AppWidgetUpdater
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AddEditRecordViewModel @Inject constructor(
    private val recordDao: RecordDao,
    private val recordToBadgeRelationDao: RecordToBadgeRelationDao,
    private val listAppWidgetUpdater: AppWidgetUpdater,
    private val currentEpochSecondsProvider: CurrentEpochSecondsProvider,
    private val savedStateHandle: SavedStateHandle
) : ViewModel() {
    private val _expressionErrorFlow = MutableStateFlow<AddEditRecordMessage?>(null)

    val expressionErrorFlow: StateFlow<AddEditRecordMessage?>
        get() = _expressionErrorFlow

    private val currentRecordIdFlow = MutableStateFlow<Int?>(null)

    /**
     * Gets or sets id of a record to edit.
     *
     * Setter can be called multiple times but the id should remain the same.
     */
    var currentRecordId = -1
        set(value) {
            field = value

            if (value >= 0) {
                // After current record is successfully loaded, 'check expression job' will be started anyway.
                currentRecordIdFlow.value = value
            } else {
                // Only after we're sure that there's no record to load, we can update expression error if expression is
                // actually empty.
                if (expression.isBlank()) {
                    _expressionErrorFlow.value = AddEditRecordMessage.EMPTY_TEXT
                }
            }

            startCheckExpressionJobIfNeccessary()
        }

    private val currentRecordStateManager = DataLoadStateManager<RecordWithBadges>(TAG)
    val currentRecordStateFlow = currentRecordStateManager.buildFlow(viewModelScope) {
        fromFlow {
            currentRecordIdFlow.filterNotNull().map { id ->
                // If the record is null, then something is really
                // wrong with the state because the record id is pointing to just can't be deleted when the logic is loading it.
                val currentRecord = recordDao.getRecordWithBadgesById(id)!!

                setExpressionInternal(currentRecord.expression)
                additionalNotes = currentRecord.additionalNotes

                validity.mutate {
                    enable(expressionValidityField)
                    enable(meaningValidityField)
                }

                currentRecord
            }
        }
    }

    private var isCheckExprJobStarted = false

    private val checkExpressionChannel = Channel<String>(
        capacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )

    val expressionFlow = savedStateHandle.getStateFlow(KEY_EXPRESSION, "")
    val additionalNotesFlow = savedStateHandle.getStateFlow(KEY_ADDITIONAL_NOTES, "")
    val changeCreationTimeFlow = savedStateHandle.getStateFlow(KEY_CHANGE_CREATION_TIME, true)

    var expression: String
        get() = expressionFlow.value
        set(value) {
            if (expressionFlow.value != value) {
                setExpressionInternal(value)

                validity.mutate { disable(expressionValidityField, isComputed = false) }
                checkExpressionChannel.trySend(value)
            }
        }

    var additionalNotes: String
        get() = additionalNotesFlow.value
        set(value) {
            savedStateHandle[KEY_ADDITIONAL_NOTES] = value
        }

    var changeCreationTime: Boolean
        get() = changeCreationTimeFlow.value
        set(value) {
            savedStateHandle[KEY_CHANGE_CREATION_TIME] = value
        }

    var getMeaning: (() -> ComplexMeaning)? = null
    var getBadges: (() -> Array<RecordBadgeInfo>)? = null

    val validity = ValidityFlow(validityScheme)

    val addOrEditAction = viewModelAction(TAG) {
        val expr = expression.trim()
        val additionalNotes = additionalNotes.trim()
        val rawMeaning = getMeaning.invokeOrThrow().rawText
        val badges = getBadges.invokeOrThrow()
        val currentEpochSeconds = currentEpochSecondsProvider.get { Utc }

        val recordId: Int
        if (currentRecordId >= 0) {
            val newEpochSeconds = if (changeCreationTime) {
                currentEpochSeconds
            } else {
                currentRecordStateFlow.firstSuccess().epochSeconds
            }

            recordDao.update(
                currentRecordId,
                expr, rawMeaning, additionalNotes,
                newEpochSeconds
            )

            recordId = currentRecordId
        } else {
            recordDao.insert(
                Record(
                    id = 0,
                    expr, rawMeaning, additionalNotes,
                    score = 0,
                    currentEpochSeconds
                )
            )

            recordId = recordDao.getRecordIdByExpression(expr)!!
        }

        recordToBadgeRelationDao.deleteAllByRecordId(recordId)
        recordToBadgeRelationDao.insertAll(
            badges.mapToArray {
                RecordToBadgeRelation(0, recordId, it.id)
            }
        )

        listAppWidgetUpdater.updateAllWidgets()
    }

    fun retryLoadCurrentRecord() {
        currentRecordStateManager.retry()
    }

    private fun setExpressionInternal(value: String) {
        savedStateHandle[KEY_EXPRESSION] = value
    }

    // Should be invoked only after currentRecordId is initialized.
    private fun startCheckExpressionJobIfNeccessary() {
        if (!isCheckExprJobStarted) {
            isCheckExprJobStarted = true

            viewModelScope.launch(Dispatchers.Default) {
                val expressions = recordDao.getAllExpressions()

                // Sort expressions to make binary search work.
                //
                // SQL's ORDER BY can't be used, because apparently it uses different algorithm to order strings
                // and it isn't compatible with string sorting algorithm in Android JVM.
                expressions.sort()

                val currentRecordExpression = if (currentRecordId >= 0) {
                    currentRecordStateFlow.firstSuccess().expression
                } else {
                    null
                }

                while (isActive) {
                    val expr = checkExpressionChannel.receive().trimToString()

                    val isEmpty = expr.isEmpty()
                    val isMeaningfulExpr = expr.containsLetterOrDigit()

                    // If we are is edit mode (currentRecordExpression is not null then),
                    // input expression shouldn't be considered as "existing"
                    // even if it does exist to allow editing meaning, origin or notes and not expression.
                    val isValid = !isEmpty &&
                            isMeaningfulExpr &&
                            (currentRecordExpression == expr || expressions.binarySearch(expr) < 0)

                    validity.mutate {
                        set(expressionValidityField, isValid, isComputed = true)
                    }

                    _expressionErrorFlow.value = when {
                        isValid -> null
                        isEmpty -> AddEditRecordMessage.EMPTY_TEXT
                        !isMeaningfulExpr -> AddEditRecordMessage.EXPRESSION_NO_LETTER_OR_DIGIT
                        else -> AddEditRecordMessage.EXISTING_EXPRESSION
                    }
                }
            }
        }
    }

    /**
     * Adds/Edits the record to/in the datastore. The actual logic is run when validity is valid
     */
    fun addOrEditRecord() {
        addOrEditAction.runWhenValid(validity)
    }

    companion object {
        private const val TAG = "AddExpressionVM"

        private const val KEY_EXPRESSION = "io.github.pelmenstar1.digiDict.AddEditRecordViewModel.expression"
        private const val KEY_ADDITIONAL_NOTES = "io.github.pelmenstar1.digiDict.AddEditRecordViewModel.notes"
        private const val KEY_CHANGE_CREATION_TIME =
            "io.github.pelmenstar1.digiDict.AddEditRecordViewModel.changeCreationTime"

        val expressionValidityField = ValidityFlow.Field(ordinal = 0)
        val meaningValidityField = ValidityFlow.Field(ordinal = 1)

        private val validityScheme = ValidityFlow.Scheme(expressionValidityField, meaningValidityField)

        internal fun <T> (() -> T)?.invokeOrThrow(): T {
            return requireNotNull(this).invoke()
        }
    }
}