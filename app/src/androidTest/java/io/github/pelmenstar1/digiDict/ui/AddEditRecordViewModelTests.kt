package io.github.pelmenstar1.digiDict.ui

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.pelmenstar1.digiDict.common.LocaleProvider
import io.github.pelmenstar1.digiDict.common.firstSuccess
import io.github.pelmenstar1.digiDict.common.time.CurrentEpochSecondsProvider
import io.github.pelmenstar1.digiDict.common.time.SystemEpochSecondsProvider
import io.github.pelmenstar1.digiDict.data.*
import io.github.pelmenstar1.digiDict.ui.addEditRecord.AddEditRecordMessage
import io.github.pelmenstar1.digiDict.ui.addEditRecord.AddEditRecordViewModel
import io.github.pelmenstar1.digiDict.utils.*
import io.github.pelmenstar1.digiDict.widgets.AppWidgetUpdater
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.AfterClass
import org.junit.Before
import org.junit.BeforeClass
import org.junit.Test
import org.junit.runner.RunWith
import java.util.*
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertNull

@RunWith(AndroidJUnit4::class)
class AddEditRecordViewModelTests {
    @Before
    fun before() {
        db.reset()
    }

    private fun createViewModel(
        recordDao: RecordDao = db.recordDao(),
        preparedRecordDao: SearchPreparedRecordDao = db.searchPreparedRecordDao(),
        recordToBadgeRelationDao: RecordToBadgeRelationDao = db.recordToBadgeRelationDao(),
        appWidgetUpdater: AppWidgetUpdater = AppWidgetUpdaterStub,
        currentEpochSecondsProvider: CurrentEpochSecondsProvider = SystemEpochSecondsProvider,
        localeProvider: LocaleProvider = LocaleProvider.fromValue(Locale.ROOT)
    ): AddEditRecordViewModel {
        return AddEditRecordViewModel(
            recordDao,
            preparedRecordDao,
            recordToBadgeRelationDao,
            appWidgetUpdater,
            currentEpochSecondsProvider,
            localeProvider
        )
    }

    private inline fun useViewModel(
        recordDao: RecordDao = db.recordDao(),
        preparedRecordDao: SearchPreparedRecordDao = db.searchPreparedRecordDao(),
        recordToBadgeRelationDao: RecordToBadgeRelationDao = db.recordToBadgeRelationDao(),
        appWidgetUpdater: AppWidgetUpdater = AppWidgetUpdaterStub,
        currentEpochSecondsProvider: CurrentEpochSecondsProvider = SystemEpochSecondsProvider,
        localeProvider: LocaleProvider = LocaleProvider.fromValue(Locale.ROOT),
        block: (AddEditRecordViewModel) -> Unit
    ) {
        createViewModel(
            recordDao,
            preparedRecordDao,
            recordToBadgeRelationDao,
            appWidgetUpdater,
            currentEpochSecondsProvider,
            localeProvider
        ).use(block)
    }

    private fun createRecord(expression: String): Record {
        return Record(
            id = 0,
            expression = expression,
            meaning = "C123",
            additionalNotes = "",
            score = 0, epochSeconds = 0
        )
    }

    private suspend fun AddEditRecordViewModel.getValidity(): Int {
        return validity
            .filterNotNull()
            .filter { (it and AddEditRecordViewModel.EXPRESSION_VALIDITY_NOT_CHOSEN_BIT) == 0 }
            .first()
    }

    private suspend fun assertExpressionInvalid(vm: AddEditRecordViewModel, expectedMsg: AddEditRecordMessage) {
        val validity = vm.getValidity()

        assertEquals(0, validity and AddEditRecordViewModel.EXPRESSION_VALIDITY_BIT)
        assertEquals(expectedMsg, vm.expressionErrorFlow.filterNotNull().first())
    }

    private suspend fun assertExpressionValid(vm: AddEditRecordViewModel) {
        val validity = vm.getValidity()

        assertNotEquals(0, validity and AddEditRecordViewModel.EXPRESSION_VALIDITY_BIT)
        assertNull(vm.expressionErrorFlow.first())
    }

    @Test
    fun expressionIsInvalidWhenEmptyTest() = runTest {
        useViewModel { vm ->
            vm.expression = "123"
            vm.expression = " "

            assertExpressionInvalid(vm, AddEditRecordMessage.EMPTY_TEXT)
        }
    }

    @Test
    fun expressionIsInvalidWhenDbContainsItTest() = runTest {
        val dao = db.recordDao()
        dao.insertAll(
            arrayOf(
                createRecord("Expr1"),
                createRecord("Expr2"),
                createRecord("Expr3")
            )
        )

        useViewModel { vm ->
            vm.expression = "Expr2"

            assertExpressionInvalid(vm, AddEditRecordMessage.EXISTING_EXPRESSION)
        }
    }

    @Test
    fun expressionIsValidWhenDbDoesNotContainsItTest() = runTest {
        val recordDao = db.recordDao()
        recordDao.insertAll(
            arrayOf(
                createRecord("Expr1"),
                createRecord("Expr2"),
                createRecord("Expr3")
            )
        )

        useViewModel { vm ->
            vm.expression = "Expr4"

            assertExpressionValid(vm)
        }
    }

    @Test
    fun expressionIsValidWhenItEqualsToCurrentRecordExpressionTest() = runTest {
        val recordDao = db.recordDao()
        recordDao.insertAll(
            arrayOf(
                createRecord("Expr1"),
                createRecord("Expr2"),
                createRecord("Expr3")
            )
        )

        useViewModel { vm ->
            val expectedCurrentRecord = recordDao.getRecordWithBadgesByExpression("Expr2")!!
            vm.currentRecordId = expectedCurrentRecord.id

            val actualCurrentRecord = vm.currentRecordStateFlow.firstSuccess()
            assertEquals(expectedCurrentRecord, actualCurrentRecord)

            vm.expression = "Expr2"

            assertExpressionValid(vm)
        }
    }

    @Test
    fun currentRecordTest() = runTest {
        val dao = db.recordDao()
        dao.insertAll(
            arrayOf(
                createRecord("Expr1"),
                createRecord("Expr2"),
                createRecord("Expr3")
            )
        )

        val expectedRecord = dao.getRecordById(1)

        useViewModel { vm ->
            vm.currentRecordId = 1

            val actualRecord = vm.currentRecordStateFlow.firstSuccess()

            assertEquals(expectedRecord, actualRecord)
        }
    }

    @Test
    fun addRecordTest() = runTest {
        suspend fun testCase(expectedMeaning: ComplexMeaning, expectedBadges: Array<RecordBadgeInfo>) {
            val recordDao = db.recordDao()
            val recordBadgeDao = db.recordBadgeDao()
            val vm = createViewModel()

            val expectedExpr = "Expr1"
            val expectedNotes = "Notes"

            vm.expression = expectedExpr
            vm.additionalNotes = expectedNotes
            vm.getMeaning = { expectedMeaning }
            vm.getBadges = { expectedBadges }

            recordBadgeDao.insertAll(expectedBadges)
            vm.addOrEditAction.runAndWaitForResult()

            val actualRecord = recordDao.getRecordWithBadgesByExpression(expectedExpr)!!

            assertEquals(expectedExpr, actualRecord.expression)
            assertEquals(expectedMeaning, ComplexMeaning.parse(actualRecord.meaning))
            assertEquals(expectedNotes, actualRecord.additionalNotes)
            assertContentEquals(expectedBadges, actualRecord.badges)
            assertEquals(0, actualRecord.score)

            db.reset()
            vm.clearThroughReflection()
        }

        suspend fun testCase(expectedBadges: Array<RecordBadgeInfo>) {
            testCase(ComplexMeaning.Common("Meaning"), expectedBadges)
            testCase(ComplexMeaning.List("M1", "M2", "M3"), expectedBadges)
            testCase(ComplexMeaning.List("M1", "M2"), expectedBadges)
        }

        testCase(expectedBadges = emptyArray())
        testCase(expectedBadges = arrayOf(RecordBadgeInfo(id = 1, name = "Badge1", outlineColor = 1)))
        testCase(
            expectedBadges = arrayOf(
                RecordBadgeInfo(id = 1, name = "Badge1", outlineColor = 1),
                RecordBadgeInfo(id = 2, name = "Badge2", outlineColor = 2)
            )
        )
    }

    @Test
    fun editRecordTest() = runTest {
        suspend fun testCase(oldBadges: Array<RecordBadgeInfo>, newBadges: Array<RecordBadgeInfo>) {
            val recordDao = db.recordDao()
            val recordBadgeDao = db.recordBadgeDao()

            val expectedNewEpochSeconds = 100L
            val vm = createViewModel(
                currentEpochSecondsProvider = FakeCurrentEpochSecondsProvider(expectedNewEpochSeconds)
            )

            val recordId = 1
            val oldRecord = db.addRecordAndBadges(
                Record(
                    recordId,
                    "Expr1_Old",
                    "CMeaning1_Old",
                    "Notes1_Old",
                    1,
                    0
                ),
                badges = oldBadges
            )

            vm.currentRecordId = recordId

            // Wait until current record is loaded
            vm.currentRecordStateFlow.firstSuccess()

            val expectedNewRecord = RecordWithBadges(
                recordId,
                "Expr1_New",
                "CMeaning1_New",
                "Notes1_New",
                oldRecord.score,
                expectedNewEpochSeconds,
                badges = newBadges
            )

            for (newBadge in newBadges) {
                if (oldBadges.indexOfFirst { it.id == newBadge.id } >= 0) {
                    recordBadgeDao.update(newBadge)
                } else {
                    recordBadgeDao.insert(newBadge)
                }
            }

            vm.expression = expectedNewRecord.expression
            vm.additionalNotes = expectedNewRecord.additionalNotes
            vm.getMeaning = { ComplexMeaning.parse(expectedNewRecord.meaning) }
            vm.getBadges = { newBadges }

            vm.addOrEditAction.runAndWaitForResult()

            val actualRecord = recordDao.getRecordWithBadgesById(recordId)
            assertEquals(expectedNewRecord, actualRecord)

            db.reset()
            vm.clearThroughReflection()
        }

        testCase(
            oldBadges = emptyArray(),
            newBadges = arrayOf(RecordBadgeInfo(1, "Badge1", 1))
        )

        testCase(
            oldBadges = arrayOf(RecordBadgeInfo(1, "Badge1_Old", 2)),
            newBadges = arrayOf(RecordBadgeInfo(1, "Badge1_New", 1))
        )

        testCase(
            oldBadges = arrayOf(RecordBadgeInfo(1, "Badge1_Old", 2)),
            newBadges = arrayOf(
                RecordBadgeInfo(1, "Badge1_New", 1),
                RecordBadgeInfo(2, "Badge2_New", 3)
            ),
        )
        testCase(
            oldBadges = arrayOf(RecordBadgeInfo(1, "Badge1", 1)),
            newBadges = emptyArray()
        )
    }

    companion object {
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