package io.github.pelmenstar1.digiDict.ui.addEditRecord

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavController
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.pelmenstar1.digiDict.data.AppDatabase
import io.github.pelmenstar1.digiDict.data.ComplexMeaning
import io.github.pelmenstar1.digiDict.data.Record
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
import java.util.*
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject

@HiltViewModel
class AddEditRecordViewModel @Inject constructor(
    appDatabase: AppDatabase,
    private val listAppWidgetUpdater: AppWidgetUpdater
) : ViewModel() {
    private val recordDao = appDatabase.recordDao()

    val invalidity = MutableStateFlow(MEANING_INVALIDITY_BIT or EXPRESSION_INVALIDITY_BIT)

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

    private var _newExpression = ""

    var newExpression: CharSequence
        get() = _newExpression
        set(value) {
            setExpressionInternal(value.trimToString())
        }

    var getMeaning: (() -> ComplexMeaning)? = null

    var newAdditionalNotes: CharSequence = ""

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
            try {
                recordDao.getRecordById(currentRecordId).also {
                    _currentRecordFlow.value = Result.success(it)
                }

                startCheckExprJobIfNecessary()
            } catch (e: Exception) {
                _currentRecordFlow.value = Result.failure(e)
            }
        }
    }

    fun initErrors() {
        // If there's a 'current record', expression can't be blank and hence no error is needed.
        if (currentRecordId < 0 && _newExpression.isBlank()) {
            _expressionErrorFlow.value = AddEditRecordMessage.EMPTY_TEXT
        }
    }

    private fun setExpressionInternal(value: String) {
        _newExpression = value

        if (value.isBlank()) {
            invalidity.withBit(EXPRESSION_INVALIDITY_BIT, true)
            _expressionErrorFlow.value = AddEditRecordMessage.EMPTY_TEXT
        } else {
            if (currentRecordId < 0 || _currentRecordFlow.value?.isSuccess == true) {
                startCheckExprJobIfNecessary()

                invalidity.withBit(EXPRESSION_INVALIDITY_BIT, true)
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
                Arrays.sort(expressions)

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
                        Log.i(
                            TAG,
                            "Expression $expr is considered valid; currentRecordExpression=$currentRecordExpression"
                        )

                        invalidity.withBit(EXPRESSION_INVALIDITY_BIT, false)
                        _expressionErrorFlow.value = null
                    } else {
                        Log.i(
                            TAG,
                            "Expression $expr is considered invalid; currentRecordExpression=$currentRecordExpression"
                        )

                        invalidity.withBit(EXPRESSION_INVALIDITY_BIT, true)
                        _expressionErrorFlow.value = AddEditRecordMessage.EXISTING_EXPRESSION
                    }
                }
            }
        }
    }

    fun searchExpression(navController: NavController) {
        val directions = AddEditRecordFragmentDirections.actionAddEditRecordToChooseRemoteDictionaryProvider(_newExpression)

        navController.navigate(directions)
    }

    fun addOrEditExpression(navController: NavController) {
        // Disallow starting a job when it has been started already.
        if (isAddJobStarted.compareAndSet(false, true)) {
            // Saving only those values which have been typed by the time of calling addOrEditExpression()
            val expr = _newExpression.trimToString()
            val additionalNotes = newAdditionalNotes.trimToString()
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

                        navController.popBackStack()
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

        const val EXPRESSION_INVALIDITY_BIT = 1
        const val MEANING_INVALIDITY_BIT = 1 shl 1
    }
}