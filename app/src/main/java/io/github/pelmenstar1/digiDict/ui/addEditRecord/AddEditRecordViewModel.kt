package io.github.pelmenstar1.digiDict.ui.addEditRecord

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.pelmenstar1.digiDict.common.*
import io.github.pelmenstar1.digiDict.common.time.CurrentEpochSecondsProvider
import io.github.pelmenstar1.digiDict.data.*
import io.github.pelmenstar1.digiDict.widgets.AppWidgetUpdater
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject

@HiltViewModel
class AddEditRecordViewModel @Inject constructor(
    private val recordDao: RecordDao,
    private val recordToBadgeRelationDao: RecordToBadgeRelationDao,
    private val listAppWidgetUpdater: AppWidgetUpdater,
    private val currentEpochSecondsProvider: CurrentEpochSecondsProvider
) : ViewModel() {
    val validity = MutableStateFlow<Int?>(null)

    private val _expressionErrorFlow = MutableStateFlow<AddEditRecordMessage?>(null)
    val expressionErrorFlow = _expressionErrorFlow.asStateFlow()

    private val currentRecordIdFlow = MutableStateFlow<Int?>(null)

    // currentRecordId can be possibly updated only one, when the fragment is started, so it's thread-safe to read it
    var currentRecordId = -1
        set(value) {
            if (field != value && value >= 0) {
                field = value

                currentRecordIdFlow.value = value
            }
        }

    private val currentRecordStateManager = DataLoadStateManager<RecordWithBadges?>(TAG)
    val currentRecordStateFlow = currentRecordStateManager.buildFlow(viewModelScope) {
        fromFlow {
            currentRecordIdFlow.filterNotNull().map { id ->
                recordDao.getRecordWithBadgesById(id).also {
                    validity.updateNullable {
                        it.withBit(EXPRESSION_VALIDITY_BIT, true).withBit(EXPRESSION_VALIDITY_NOT_CHOSEN_BIT, false)
                    }

                    startCheckExprJobIfNecessary()
                }
            }
        }
    }

    private val isCheckExpressionJobStarted = AtomicBoolean()
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
        val epochSeconds = currentEpochSecondsProvider.currentEpochSeconds()

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

    fun initErrors() {
        // If there's a 'current record', expression can't be blank and hence no error is needed.
        if (currentRecordId < 0 && _expression.isBlank()) {
            _expressionErrorFlow.value = AddEditRecordMessage.EMPTY_TEXT
        }
    }

    // setExpressionInternal shouldn't be called when there's 'current record' and it's not loaded.
    // It's not critical except 'check expression job' will be started earlier than it should be.
    private fun setExpressionInternal(value: String) {
        _expression = value

        startCheckExprJobIfNecessary()

        validity.update {
            val prevValue = it ?: 0

            prevValue
                .withBit(EXPRESSION_VALIDITY_BIT, false)
                .withBit(EXPRESSION_VALIDITY_NOT_CHOSEN_BIT, true)
        }

        checkExpressionChannel.trySend(value)
    }

    // Must not be started if there's current record and it's null at the moment of calling method
    private fun startCheckExprJobIfNecessary() {
        if (isCheckExpressionJobStarted.compareAndSet(false, true)) {
            viewModelScope.launch(Dispatchers.Default) {
                val expressions = recordDao.getAllExpressions()

                // Sort expressions to make binary search work.
                //
                // SQL's ORDER BY can't be used, because apparently it uses different algorithm to order strings
                // and it isn't compatible with string sorting algorithm in Android JVM.
                expressions.sort()

                val currentRecordExpression = if (currentRecordId >= 0) {
                    currentRecordStateFlow.firstSuccess()?.expression
                } else {
                    null
                }

                while (isActive) {
                    val expr = checkExpressionChannel.receive()

                    val isBlank = expr.isBlank()
                    val containsLetterOrDigit = expr.any { it.isLetterOrDigit() }

                    // If we are is edit mode (currentRecordExpression is not null then),
                    // input expression shouldn't be considered as "existing"
                    // even if it does exist to allow editing meaning, origin or notes and not expression.
                    val isValid = !isBlank &&
                            containsLetterOrDigit &&
                            (currentRecordExpression == expr || expressions.binarySearch(expr) < 0)

                    validity.update {
                        val prevValue = it ?: 0

                        prevValue
                            .withBit(EXPRESSION_VALIDITY_BIT, isValid)
                            .withBit(EXPRESSION_VALIDITY_NOT_CHOSEN_BIT, false)
                    }

                    _expressionErrorFlow.value = when {
                        isValid -> null
                        isBlank -> AddEditRecordMessage.EMPTY_TEXT
                        !containsLetterOrDigit -> AddEditRecordMessage.EXPRESSION_NO_LETTER_OR_DIGIT
                        else -> AddEditRecordMessage.EXISTING_EXPRESSION
                    }
                }
            }
        }
    }

    fun addOrEditRecord() = addOrEditAction.run()

    companion object {
        private const val TAG = "AddExpressionVM"

        const val EXPRESSION_VALIDITY_BIT = 1
        const val EXPRESSION_VALIDITY_NOT_CHOSEN_BIT = 1 shl 2
        const val MEANING_VALIDITY_BIT = 1 shl 1

        const val ALL_VALID_MASK = EXPRESSION_VALIDITY_BIT or MEANING_VALIDITY_BIT

        internal fun <T> (() -> T)?.invokeOrThrow(): T {
            return requireNotNull(this).invoke()
        }
    }
}