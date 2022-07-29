package io.github.pelmenstar1.digiDict.ui.addEditRecord

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.pelmenstar1.digiDict.data.AppDatabase
import io.github.pelmenstar1.digiDict.data.ComplexMeaning
import io.github.pelmenstar1.digiDict.data.Record
import io.github.pelmenstar1.digiDict.utils.Event
import io.github.pelmenstar1.digiDict.utils.trimToString
import io.github.pelmenstar1.digiDict.utils.withBit
import io.github.pelmenstar1.digiDict.widgets.AppWidgetUpdater
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject

@HiltViewModel
class AddEditRecordViewModel @Inject constructor(
    appDatabase: AppDatabase,
    private val listAppWidgetUpdater: AppWidgetUpdater
) : ViewModel() {
    private val recordDao = appDatabase.recordDao()

    // In initial state, VM's state is invalid.
    val validity = MutableStateFlow(0)

    private val _expressionErrorFlow = MutableStateFlow<AddEditRecordMessage?>(null)
    private val _dbErrorFlow = MutableStateFlow<AddEditRecordMessage?>(null)
    private val _currentRecordFlow = MutableStateFlow<Result<Record?>?>(null)

    val expressionErrorFlow = _expressionErrorFlow.asStateFlow()
    val dbErrorFlow = _dbErrorFlow.asStateFlow()
    val currentRecordFlow = _currentRecordFlow.asStateFlow()

    private val isCheckExpressionJobStarted = AtomicBoolean()
    private val checkExpressionChannel = Channel<String>(
        capacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )

    private val isAddJobStarted = AtomicBoolean()

    val onRecordSuccessfullyAdded = Event()

    private var _expression = ""

    var expression: CharSequence
        get() = _expression
        set(value) {
            setExpressionInternal(value.trimToString())
        }

    var getMeaning: (() -> ComplexMeaning)? = null

    var additionalNotes: CharSequence = ""

    // currentRecordId can be possibly updated only one, when the fragment is started, so it's thread-safe to read it
    var currentRecordId = -1
        set(value) {
            if (field != value && value >= 0) {
                field = value

                loadCurrentRecord()
            }
        }

    fun loadCurrentRecord() {
        viewModelScope.launch(Dispatchers.IO) {
            _currentRecordFlow.value = runCatching {
                recordDao.getRecordById(currentRecordId).also {
                    startCheckExprJobIfNecessary()
                }
            }
        }
    }

    fun initErrors() {
        // If there's a 'current record', expression can't be blank and hence no error is needed.
        if (currentRecordId < 0 && _expression.isBlank()) {
            _expressionErrorFlow.value = AddEditRecordMessage.EMPTY_TEXT
        }
    }

    private fun setExpressionInternal(value: String) {
        _expression = value

        if (value.isBlank()) {
            validity.withBit(EXPRESSION_VALIDITY_BIT, false)
            _expressionErrorFlow.value = AddEditRecordMessage.EMPTY_TEXT
        } else {
            if (currentRecordId < 0 || _currentRecordFlow.value?.isSuccess == true) {
                startCheckExprJobIfNecessary()

                validity.withBit(EXPRESSION_VALIDITY_BIT, false)
                checkExpressionChannel.trySend(value)
            }
        }
    }

    // Must not be started if there's current record and it's null at the moment of calling method
    private fun startCheckExprJobIfNecessary() {
        if (isCheckExpressionJobStarted.compareAndSet(false, true)) {
            viewModelScope.launch(Dispatchers.Default) {
                val expressions = recordDao.getAllExpressions()

                // Sort expressions to make binary search work.
                expressions.sort()

                val currentRecordExpression = if (currentRecordId >= 0) {
                    requireNotNull(_currentRecordFlow.value).getOrThrow()?.expression
                } else {
                    null
                }

                while (isActive) {
                    val expr = checkExpressionChannel.receive()

                    // If we are is edit mode (currentRecordExpression is not null then),
                    // then if input expression shouldn't be considered as "existing"
                    // even if it does exist to allow editing meaning, origin or notes and not expression.
                    if (currentRecordExpression == expr || expressions.binarySearch(expr) < 0) {
                        validity.withBit(EXPRESSION_VALIDITY_BIT, true)
                        _expressionErrorFlow.value = null
                    } else {
                        validity.withBit(EXPRESSION_VALIDITY_BIT, false)
                        _expressionErrorFlow.value = AddEditRecordMessage.EXISTING_EXPRESSION
                    }
                }
            }
        }
    }

    fun addOrEditExpression() {
        // Disallow starting a job when it has been started already.
        if (isAddJobStarted.compareAndSet(false, true)) {
            // Saving only those values which have been typed by the time of calling addOrEditExpression()
            val expr = _expression.trimToString()
            val additionalNotes = additionalNotes.trimToString()
            val rawMeaning = requireNotNull(getMeaning).invoke().rawText

            viewModelScope.launch(Dispatchers.IO) {
                try {
                    val epochSeconds = System.currentTimeMillis() / 1000

                    if (currentRecordId >= 0) {
                        recordDao.update(
                            currentRecordId,
                            expr, rawMeaning, additionalNotes,
                            epochSeconds
                        )
                    } else {
                        recordDao.insert(
                            Record(
                                id = 0,
                                expr, rawMeaning, additionalNotes,
                                score = 0,
                                epochSeconds = epochSeconds
                            )
                        )
                    }

                    withContext(Dispatchers.Main) {
                        listAppWidgetUpdater.updateAllWidgets()

                        onRecordSuccessfullyAdded.raise()
                    }

                    _dbErrorFlow.value = null

                    // If there's no exception, then isAddJobStarted shouldn't be set to false,
                    // because view-model will be destroyed soon.
                } catch (e: Exception) {
                    Log.e(TAG, null, e)
                    isAddJobStarted.set(false)

                    _dbErrorFlow.value = AddEditRecordMessage.DB_ERROR
                }
            }
        }
    }

    companion object {
        private const val TAG = "AddExpressionVM"

        const val EXPRESSION_VALIDITY_BIT = 1
        const val MEANING_VALIDITY_BIT = 1 shl 1
        const val ALL_VALID_MASK = EXPRESSION_VALIDITY_BIT or MEANING_VALIDITY_BIT
    }
}