package io.github.pelmenstar1.digiDict.ui.quiz

import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.pelmenstar1.digiDict.common.DataLoadState
import io.github.pelmenstar1.digiDict.common.DataLoadStateManager
import io.github.pelmenstar1.digiDict.common.FixedBitSet
import io.github.pelmenstar1.digiDict.common.android.TextBreakAndHyphenationInfoSource
import io.github.pelmenstar1.digiDict.common.android.viewModelAction
import io.github.pelmenstar1.digiDict.common.time.CurrentEpochSecondsProvider
import io.github.pelmenstar1.digiDict.common.time.SECONDS_IN_DAY
import io.github.pelmenstar1.digiDict.common.time.get
import io.github.pelmenstar1.digiDict.common.ui.SingleDataLoadStateViewModel
import io.github.pelmenstar1.digiDict.data.ConciseRecordWithBadges
import io.github.pelmenstar1.digiDict.data.RecordDao
import io.github.pelmenstar1.digiDict.prefs.DigiDictAppPreferences
import kotlinx.coroutines.flow.*
import javax.inject.Inject
import kotlin.random.Random

@HiltViewModel
class QuizViewModel @Inject constructor(
    private val recordDao: RecordDao,
    private val appPreferences: DigiDictAppPreferences,
    private val currentEpochSecondsProvider: CurrentEpochSecondsProvider,
    val textBreakAndHyphenationInfoSource: TextBreakAndHyphenationInfoSource
) : SingleDataLoadStateViewModel<Array<ConciseRecordWithBadges>>(TAG) {
    override val canRefreshAfterSuccess: Boolean
        get() = false

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

    val saveAction = viewModelAction(TAG) {
        // Relies on the fact that user can't answer if result is null
        val inputState = dataStateFlow.first()

        if (inputState !is DataLoadState.Success<Array<ConciseRecordWithBadges>>) {
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
    }

    override fun DataLoadStateManager.FlowBuilder<Array<ConciseRecordWithBadges>>.buildDataFlow() = fromFlow {
        modeFlow.filterNotNull().map { selectedMode ->
            val records = if (selectedMode == QuizMode.ALL) {
                recordDao.getRandomConciseRecordsWithBadges(random, RECORDS_MAX_SIZE)
            } else {
                val duration = when (selectedMode) {
                    QuizMode.LAST_24_HOURS -> SECONDS_IN_DAY
                    QuizMode.LAST_48_HOURS -> 2 * SECONDS_IN_DAY
                    // It's impossible but compiler doesn't know about it
                    else -> throw RuntimeException("Impossible")
                }

                val currentEpochSeconds = currentEpochSecondsProvider.get { Utc }

                recordDao.getRandomConciseRecordsWithBadgesAfter(
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

    fun saveResults() = saveAction.run()

    companion object {
        private const val TAG = "QuizViewModel"
        const val RECORDS_MAX_SIZE = 10

        private val random = Random(System.currentTimeMillis())
    }
}