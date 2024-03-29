package io.github.pelmenstar1.digiDict.ui

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.pelmenstar1.digiDict.common.android.NoOpTextBreakAndHyphenationInfoSource
import io.github.pelmenstar1.digiDict.common.time.CurrentEpochSecondsProvider
import io.github.pelmenstar1.digiDict.common.time.SystemEpochSecondsProvider
import io.github.pelmenstar1.digiDict.commonTestUtils.use
import io.github.pelmenstar1.digiDict.commonTestUtils.waitUntilSuccessOrThrowOnError
import io.github.pelmenstar1.digiDict.data.AppDatabase
import io.github.pelmenstar1.digiDict.data.Record
import io.github.pelmenstar1.digiDict.data.RecordDao
import io.github.pelmenstar1.digiDict.prefs.DigiDictAppPreferences
import io.github.pelmenstar1.digiDict.ui.quiz.QuizMode
import io.github.pelmenstar1.digiDict.ui.quiz.QuizViewModel
import io.github.pelmenstar1.digiDict.utils.AppDatabaseUtils
import io.github.pelmenstar1.digiDict.utils.ReadonlyDigiDictAppPreferences
import io.github.pelmenstar1.digiDict.utils.reset
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.AfterClass
import org.junit.Before
import org.junit.BeforeClass
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.test.assertEquals

@RunWith(AndroidJUnit4::class)
class QuizViewModelTests {
    @Before
    fun before() {
        db.reset()
    }

    private fun createViewModel(
        recordDao: RecordDao = db.recordDao(),
        appPreferences: DigiDictAppPreferences = ReadonlyDigiDictAppPreferences(DEFAULT_SNAPSHOT),
        currentEpochSecondsProvider: CurrentEpochSecondsProvider = SystemEpochSecondsProvider
    ): QuizViewModel {
        return QuizViewModel(
            recordDao,
            appPreferences,
            currentEpochSecondsProvider,
            NoOpTextBreakAndHyphenationInfoSource
        )
    }

    private fun createRecord(expression: String): Record {
        return Record(
            id = 0,
            expression = expression,
            meaning = "CMeaning",
            additionalNotes = "AdditionalNotes",
            score = 0,
            epochSeconds = 0
        )
    }

    @Test
    fun isAllAnsweredTest() = runTest {
        val dao = db.recordDao()

        dao.insertAll(
            arrayOf(
                createRecord("Expr1"),
                createRecord("Expr2"),
                createRecord("Expr3"),
                createRecord("Expr4")
            )
        )

        suspend fun testCase(indices: IntArray, expectedResult: Boolean) {
            val vm = createViewModel(dao)

            vm.use {
                vm.mode = QuizMode.ALL

                vm.dataStateFlow.waitUntilSuccessOrThrowOnError()

                indices.forEach {
                    vm.onItemAnswer(it, false)
                }

                assertEquals(expectedResult, vm.isAllAnswered.first())
            }
        }


        testCase(intArrayOf(), expectedResult = false)
        testCase(intArrayOf(0, 1, 2), expectedResult = false)
        testCase(intArrayOf(0, 3), expectedResult = false)

        testCase(intArrayOf(0, 1, 2, 3), expectedResult = true)
        testCase(intArrayOf(3, 2, 1, 0), expectedResult = true)
        testCase(intArrayOf(0, 2, 1, 3), expectedResult = true)
    }

    companion object {
        private val DEFAULT_SNAPSHOT = DigiDictAppPreferences.Entries.run {
            DigiDictAppPreferences.Snapshot(
                scorePointsPerCorrectAnswer = scorePointsPerCorrectAnswer.defaultValue,
                scorePointsPerWrongAnswer = scorePointsPerWrongAnswer.defaultValue,
                useCustomTabs = useCustomTabs.defaultValue,
                widgetListMaxSize = widgetListMaxSize.defaultValue,
                recordTextBreakStrategy = recordTextBreakStrategy.defaultValue,
                recordTextHyphenationFrequency = recordTextHyphenationFrequency.defaultValue
            )
        }

        private lateinit var db: AppDatabase

        @BeforeClass
        @JvmStatic
        fun beforeClass() {
            val context = ApplicationProvider.getApplicationContext<Context>()

            db = AppDatabaseUtils.createTestDatabase(context)
        }

        @AfterClass
        @JvmStatic
        fun after() {
            db.close()
        }
    }
}