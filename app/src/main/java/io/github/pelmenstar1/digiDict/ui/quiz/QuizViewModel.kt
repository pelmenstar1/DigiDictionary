package io.github.pelmenstar1.digiDict.ui.quiz

import android.util.Log
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.pelmenstar1.digiDict.data.Record
import io.github.pelmenstar1.digiDict.data.RecordDao
import io.github.pelmenstar1.digiDict.prefs.AppPreferences
import io.github.pelmenstar1.digiDict.time.CurrentEpochSecondsProvider
import io.github.pelmenstar1.digiDict.time.SECONDS_IN_DAY
import io.github.pelmenstar1.digiDict.ui.SingleDataLoadStateViewModel
import io.github.pelmenstar1.digiDict.utils.DataLoadState
import io.github.pelmenstar1.digiDict.utils.DataLoadStateManager
import io.github.pelmenstar1.digiDict.utils.Event
import io.github.pelmenstar1.digiDict.utils.FixedBitSet
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.random.Random

@HiltViewModel
class QuizViewModel @Inject constructor(
    private val recordDao: RecordDao,
    private val appPreferences: AppPreferences,
    private val currentEpochSecondsProvider: CurrentEpochSecondsProvider
) : SingleDataLoadStateViewModel<Array<Record>>(TAG) {
    @Volatile
    private var answeredBits: FixedBitSet? = null

    @Volatile
    private var correctAnsweredBits: FixedBitSet? = null

    private val _isAllAnswered = MutableStateFlow(false)
    val isAllAnswered = _isAllAnswered.asStateFlow()

    private val modeFlow = MutableStateFlow<QuizMode?>(null)

    // Should be used only on main-thread.
    var mode: QuizMode?
        get() = modeFlow.value
        set(value) {
            modeFlow.value = value
        }

    val onSaveError = Event()
    val onResultSaved = Event()

    override fun DataLoadStateManager.FlowBuilder<Array<Record>>.buildDataFlow() = fromFlow {
        modeFlow.filterNotNull().map { selectedMode ->
            val records = if (selectedMode == QuizMode.ALL) {
                recordDao.getRandomRecords(random, RECORDS_MAX_SIZE)
            } else {
                val duration = when (selectedMode) {
                    QuizMode.LAST_24_HOURS -> SECONDS_IN_DAY
                    QuizMode.LAST_48_HOURS -> 2 * SECONDS_IN_DAY
                    // It's impossible but compiler doesn't know about it
                    else -> throw RuntimeException("Impossible")
                }

                val currentEpochSeconds = currentEpochSecondsProvider.currentEpochSeconds()

                recordDao.getRandomRecordsAfter(
                    random,
                    RECORDS_MAX_SIZE,
                    currentEpochSeconds - duration
                )
            }

            val size = records.size

            answeredBits = FixedBitSet(size)
            correctAnsweredBits = FixedBitSet(size)

            records
        }
    }

    fun onItemAnswer(index: Int, isCorrect: Boolean) {
        val answeredBits = answeredBits!!
        val correctAnsweredBits = correctAnsweredBits!!

        answeredBits.set(index)
        correctAnsweredBits[index] = isCorrect

        _isAllAnswered.value = answeredBits.isAllBitsSet()
    }

    fun saveResults() {
        viewModelScope.launch {
            try {
                // Relies on the fact that user can't answer if result is null
                val inputState = dataStateFlow.first()

                if (inputState !is DataLoadState.Success<Array<Record>>) {
                    throw IllegalStateException("Input is expected to be successful")
                }

                val (input) = inputState

                // Results can be saved only when all questions are answered, user can't re-answer, which means
                // by time of executing saveResults() correctAnsweredBits can't be changed.
                //
                // Also, if result is not null, it means correctAnsweredBits is not null as well.
                val correctAnsweredBits = requireNotNull(correctAnsweredBits)

                val snapshot = appPreferences.getSnapshotFlow().first()

                val scorePointsPerCorrectAnswer = snapshot.scorePointsPerCorrectAnswer
                val scorePointsPerWrongAnswer = snapshot.scorePointsPerWrongAnswer

                val newScores = IntArray(input.size)
                for (i in input.indices) {
                    val currentScore = input[i].score

                    val newScore = currentScore + if (correctAnsweredBits[i])
                        scorePointsPerCorrectAnswer
                    else
                        -scorePointsPerWrongAnswer

                    newScores[i] = newScore
                }

                recordDao.updateScores(input, newScores)

                onResultSaved.raiseOnMainThread()
            } catch (e: Exception) {
                Log.e(TAG, "during save", e)

                onSaveError.raiseOnMainThread()
            }
        }
    }

    companion object {
        private const val TAG = "QuizViewModel"
        const val RECORDS_MAX_SIZE = 10

        private val random = Random(System.currentTimeMillis())
    }
}