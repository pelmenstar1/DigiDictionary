package io.github.pelmenstar1.digiDict.ui.addEditRecord

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.pelmenstar1.digiDict.common.*
import io.github.pelmenstar1.digiDict.common.time.CurrentEpochSecondsProvider
import io.github.pelmenstar1.digiDict.common.time.get
import io.github.pelmenstar1.digiDict.data.*
import io.github.pelmenstar1.digiDict.widgets.AppWidgetUpdater
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
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
    private val currentEpochSecondsProvider: CurrentEpochSecondsProvider
) : ViewModel() {
    // By default, expression and meaning are invalid but it's "real" validity state for expression and meaning, so
    // VALIDITY_COMPUTED_BIT should be set.
    val validity = ValidityFlow(validityScheme)

    private val _expressionErrorFlow = MutableStateFlow<AddEditRecordMessage?>(null)
    val expressionErrorFlow = _expressionErrorFlow.asStateFlow()

    private val currentRecordIdFlow = MutableStateFlow<Int?>(null)

    // currentRecordId should be set once, when the fragment is created, so it's thread-safe to read it
    var currentRecordId = -1
        set(value) {
            field = value

            if (value >= 0) {
                // After current record is successfully loaded, 'check expression job' will be started anyway.
                currentRecordIdFlow.value = value
            } else {
                // Only after we're sure the ViewModel is not in edit-mode (there's no record to edit), we can safely
                // start 'check expression job' and show the error message about expression being empty.
                // On the contrary, if there's a record to load, the expression just can't be empty and the error message shouldn't be shown.
                startCheckExpressionJob()

                _expressionErrorFlow.value = AddEditRecordMessage.EMPTY_TEXT
            }
        }

    private val currentRecordStateManager = DataLoadStateManager<RecordWithBadges>(TAG)
    val currentRecordStateFlow = currentRecordStateManager.buildFlow(viewModelScope) {
        fromFlow {
            currentRecordIdFlow.filterNotNull().map { id ->
                // !! will throw an expression if the record is actually null but in that case something is really
                // wrong with the state because the record id is pointing to just can't be deleted when the logic is loading it.
                val currentRecord = recordDao.getRecordWithBadgesById(id)!!

                validity.mutate { enable(expressionValidityField) }

                startCheckExpressionJob()

                currentRecord
            }
        }
    }

    private val checkExpressionChannel = Channel<String>(
        capacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )

    private var _expression = ""

    var expression: CharSequence
        get() = _expression
        set(value) {
            setExpressionInternal(value.trimToString())
        }

    var getMeaning: (() -> ComplexMeaning)? = null
    var getBadges: (() -> Array<RecordBadgeInfo>)? = null
    var additionalNotes: CharSequence = ""

    val addOrEditAction = viewModelAction(TAG) {
        val expr = _expression.trimToString()
        val additionalNotes = additionalNotes.trimToString()
        val rawMeaning = getMeaning.invokeOrThrow().rawText
        val badges = getBadges.invokeOrThrow()
        val epochSeconds = currentEpochSecondsProvider.get { Utc }

        val recordId: Int
        if (currentRecordId >= 0) {
            recordDao.update(
                currentRecordId,
                expr, rawMeaning, additionalNotes,
                epochSeconds
            )

            recordId = currentRecordId
        } else {
            recordDao.insert(
                Record(
                    id = 0,
                    expr, rawMeaning, additionalNotes,
                    score = 0,
                    epochSeconds
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

    // setExpressionInternal shouldn't be called when there's 'current record' and it's not loaded.
    // It's not critical except 'check expression job' will be started earlier than it should be.
    private fun setExpressionInternal(value: String) {
        _expression = value

        validity.mutate { disable(expressionValidityField, isComputed = false) }
        checkExpressionChannel.trySend(value)
    }

    // Must not be started if there's current record and it's null at the moment of calling the method
    private fun startCheckExpressionJob() {
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
                val expr = checkExpressionChannel.receive()

                val isBlank = expr.isBlank()
                val isMeaningfulExpr = expr.containsLetterOrDigit()

                // If we are is edit mode (currentRecordExpression is not null then),
                // input expression shouldn't be considered as "existing"
                // even if it does exist to allow editing meaning, origin or notes and not expression.
                val isValid = !isBlank &&
                        isMeaningfulExpr &&
                        (currentRecordExpression == expr || expressions.binarySearch(expr) < 0)

                validity.mutate {
                    set(expressionValidityField, isValid, isComputed = true)
                }

                _expressionErrorFlow.value = when {
                    isValid -> null
                    isBlank -> AddEditRecordMessage.EMPTY_TEXT
                    !isMeaningfulExpr -> AddEditRecordMessage.EXPRESSION_NO_LETTER_OR_DIGIT
                    else -> AddEditRecordMessage.EXISTING_EXPRESSION
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

        val expressionValidityField = ValidityFlow.Field(ordinal = 0)
        val meaningValidityField = ValidityFlow.Field(ordinal = 1)

        private val validityScheme = ValidityFlow.Scheme(expressionValidityField, meaningValidityField)

        internal fun <T> (() -> T)?.invokeOrThrow(): T {
            return requireNotNull(this).invoke()
        }
    }
}