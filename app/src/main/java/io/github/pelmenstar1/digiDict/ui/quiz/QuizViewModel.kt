package io.github.pelmenstar1.digiDict.ui.quiz

import android.util.Log
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.pelmenstar1.digiDict.SECONDS_IN_DAY
import io.github.pelmenstar1.digiDict.data.AppDatabase
import io.github.pelmenstar1.digiDict.data.Record
import io.github.pelmenstar1.digiDict.scorePointsPerCorrectAnswer
import io.github.pelmenstar1.digiDict.scorePointsPerWrongAnswer
import io.github.pelmenstar1.digiDict.utils.Event
import io.github.pelmenstar1.digiDict.utils.isBitAtPositionSet
import io.github.pelmenstar1.digiDict.utils.lowestNBitsSet
import io.github.pelmenstar1.digiDict.utils.withBitAtPosition
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.random.Random

@HiltViewModel
class QuizViewModel @Inject constructor(
    appDatabase: AppDatabase,
    private val dataStore: DataStore<Preferences>,
) : ViewModel() {
    private val recordDao = appDatabase.recordDao()

    private var answeredBits = 0

    @Volatile
    private var correctAnsweredBits = 0

    // Can be changed only once, in startLoadingElements().
    @Volatile
    private var allAnsweredMask = 0

    private val _isAllAnswered = MutableStateFlow(false)
    val isAllAnswered = _isAllAnswered.asStateFlow()

    private val _result = MutableStateFlow<Array<Record>?>(null)
    val result = _result.asStateFlow()

    val onLoadingError = Event()
    val onSaveError = Event()
    val onResultSaved = Event()

    // Should be used only on main-thread.
    var mode = QuizMode.ALL

    fun startLoadingElements() {
        val selectedMode = mode
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val records = if (selectedMode == QuizMode.ALL) {
                    recordDao.getRandomRecords(random, RECORDS_MAX_SIZE)
                } else {
                    val duration = when (selectedMode) {
                        QuizMode.LAST_24_HOURS -> SECONDS_IN_DAY
                        QuizMode.LAST_48_HOURS -> 2 * SECONDS_IN_DAY
                        // It's impossible but compiler doesn't know about it
                        else -> throw RuntimeException("Impossible")
                    }
                    val nowEpochSeconds = System.currentTimeMillis() / 1000

                    recordDao.getRandomRecordsAfter(
                        random,
                        RECORDS_MAX_SIZE,
                        nowEpochSeconds - duration
                    )
                }

                if (records.size > 32) {
                    throw IllegalStateException("Too many records for quiz")
                }

                allAnsweredMask = lowestNBitsSet(records.size)

                _result.value = records
            } catch (e: Exception) {
                Log.e(TAG, "during loading", e)

                onLoadingError.raiseOnMainThread()
            }
        }
    }

    fun onItemAnswer(index: Int, isCorrect: Boolean) {
        answeredBits = answeredBits.withBitAtPosition(index, true)

        if (isCorrect) {
            correctAnsweredBits = correctAnsweredBits.withBitAtPosition(index, true)
        }

        // User can answer only when the records are loaded, allAnsweredMask is set only once, on initial load,
        // so it's safe to do such comparison 'cause allAnsweredMask can't be changed when this code is being executed.
        val mask = allAnsweredMask
        if ((answeredBits and mask) == mask) {
            _isAllAnswered.value = true
        }
    }

    fun saveResults() {
        viewModelScope.launch {
            try {
                // Relies on the fact that user can't answer if result is null
                val result = requireNotNull(_result.value)

                // Results can be saved only when all questions are answered, user can't re-answer, which means
                // by time of executing saveResults() rightAnsweredBits can't be changed.
                val correctAnswered = correctAnsweredBits

                val scorePointsPerCorrectAnswer = dataStore.scorePointsPerCorrectAnswer.first()
                val scorePointsPerWrongAnswer = dataStore.scorePointsPerWrongAnswer.first()

                val newScores = IntArray(result.size)
                for (i in result.indices) {
                    val currentScore = result[i].score
                    val newScore = currentScore + if (correctAnswered.isBitAtPositionSet(i))
                        scorePointsPerCorrectAnswer
                    else
                        -scorePointsPerWrongAnswer

                    newScores[i] = newScore
                }

                recordDao.updateScores(result, newScores)

                onResultSaved.raiseOnMainThread()
            } catch (e: Exception) {
                Log.e(TAG, "during save", e)

                onSaveError.raiseOnMainThread()
            }
        }
    }

    companion object {
        private const val TAG = "QuizViewModel"
        private const val RECORDS_MAX_SIZE = 10

        private val random = Random(System.currentTimeMillis())
    }
}