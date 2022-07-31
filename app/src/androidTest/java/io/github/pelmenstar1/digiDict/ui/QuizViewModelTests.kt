package io.github.pelmenstar1.digiDict.ui

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.pelmenstar1.digiDict.data.AppDatabase
import io.github.pelmenstar1.digiDict.data.Record
import io.github.pelmenstar1.digiDict.data.RecordDao
import io.github.pelmenstar1.digiDict.prefs.AppPreferences
import io.github.pelmenstar1.digiDict.time.CurrentEpochSecondsProvider
import io.github.pelmenstar1.digiDict.time.SystemEpochSecondsProvider
import io.github.pelmenstar1.digiDict.ui.quiz.QuizMode
import io.github.pelmenstar1.digiDict.ui.quiz.QuizViewModel
import io.github.pelmenstar1.digiDict.utils.*
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.AfterClass
import org.junit.Before
import org.junit.BeforeClass
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.random.Random
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@RunWith(AndroidJUnit4::class)
class QuizViewModelTests {
    @Before
    fun before() {
        db.reset()
    }

    private fun createViewModel(
        recordDao: RecordDao = db.recordDao(),
        appPreferences: AppPreferences = ReadonlyAppPreferences(DEFAULT_SNAPSHOT),
        currentEpochSecondsProvider: CurrentEpochSecondsProvider = SystemEpochSecondsProvider
    ): QuizViewModel {
        return QuizViewModel(recordDao, appPreferences, currentEpochSecondsProvider)
    }

    private fun createRecord(expression: String, score: Int): Record {
        return Record(
            id = 0,
            expression = expression,
            rawMeaning = "CMeaning",
            additionalNotes = "AdditionalNotes",
            score = score,
            epochSeconds = 0
        )
    }

    @Test
    fun isAllAnsweredTest() = runTest {
        val dao = db.recordDao()

        dao.insertAll(
            listOf(
                createRecord("Expr1", 0),
                createRecord("Expr2", 0),
                createRecord("Expr3", 0),
                createRecord("Expr4", 0)
            )
        )

        suspend fun testCase(indices: IntArray, expectedResult: Boolean) {
            val vm = createViewModel(dao)

            vm.use {
                vm.mode = QuizMode.ALL
                vm.startLoadingElements()

                vm.onLoadingError.handler = {
                    // Crash.
                    assertTrue(false)
                }

                // Wait until result is loaded, it's important not to do anything until then.
                vm.result.filterNotNull().first()

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

    @Test
    fun onLoadingErrorCalledOnMainThread() = runTest {
        val dao = object : RecordDaoStub() {
            override suspend fun getRandomRecords(random: Random, size: Int): Array<Record> {
                throw RuntimeException()
            }
        }

        val vm = createViewModel(recordDao = dao)
        vm.mode = QuizMode.ALL

        assertEventHandlerOnMainThread(vm, vm.onLoadingError) { vm.startLoadingElements() }
    }

    @Test
    fun onResultSavedCalledOnMainThread() = runTest {
        val dao = object : RecordDaoStub() {
            override suspend fun getRandomRecords(random: Random, size: Int): Array<Record> {
                return arrayOf(createRecord("Expr", 1))
            }
        }

        val vm = createViewModel(recordDao = dao)

        vm.mode = QuizMode.ALL
        vm.startLoadingElements()

        // Wait until result is loaded.
        vm.result.filterNotNull().first()

        assertEventHandlerOnMainThread(vm, vm.onResultSaved) { vm.saveResults() }
    }

    @Test
    fun onSaveErrorCalledOnMainThread() = runTest {
        val dao = object : RecordDaoStub() {
            override suspend fun getRandomRecords(random: Random, size: Int): Array<Record> {
                return arrayOf(createRecord("Expr1", 1))
            }

            override suspend fun updateScore(id: Int, newScore: Int) {
                throw RuntimeException()
            }
        }

        val vm = createViewModel(recordDao = dao)
        vm.mode = QuizMode.ALL
        vm.startLoadingElements()

        // Wait until result is loaded.
        vm.result.filterNotNull().first()

        assertEventHandlerOnMainThread(vm, vm.onSaveError) { saveResults() }
    }

    companion object {
        private val DEFAULT_SNAPSHOT = AppPreferences.Entries.run {
            AppPreferences.Snapshot(
                scorePointsPerCorrectAnswer = scorePointsPerCorrectAnswer.defaultValue,
                scorePointsPerWrongAnswer = scorePointsPerWrongAnswer.defaultValue,
                useCustomTabs = useCustomTabs.defaultValue
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