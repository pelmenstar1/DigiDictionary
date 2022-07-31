package io.github.pelmenstar1.digiDict.ui

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.pelmenstar1.digiDict.data.AppDatabase
import io.github.pelmenstar1.digiDict.data.ComplexMeaning
import io.github.pelmenstar1.digiDict.data.Record
import io.github.pelmenstar1.digiDict.data.RecordDao
import io.github.pelmenstar1.digiDict.time.CurrentEpochSecondsProvider
import io.github.pelmenstar1.digiDict.time.SystemEpochSecondsProvider
import io.github.pelmenstar1.digiDict.ui.addEditRecord.AddEditRecordMessage
import io.github.pelmenstar1.digiDict.ui.addEditRecord.AddEditRecordViewModel
import io.github.pelmenstar1.digiDict.utils.*
import io.github.pelmenstar1.digiDict.widgets.AppWidgetUpdater
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import org.junit.AfterClass
import org.junit.Before
import org.junit.BeforeClass
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

@RunWith(AndroidJUnit4::class)
class AddEditRecordViewModelTests {
    @Before
    fun before() {
        db.reset()
    }

    private fun createViewModel(
        recordDao: RecordDao = db.recordDao(),
        appWidgetUpdater: AppWidgetUpdater = AppWidgetUpdaterStub,
        currentEpochSecondsProvider: CurrentEpochSecondsProvider = SystemEpochSecondsProvider
    ): AddEditRecordViewModel {
        return AddEditRecordViewModel(recordDao, appWidgetUpdater, currentEpochSecondsProvider)
    }

    private inline fun useViewModel(
        recordDao: RecordDao = db.recordDao(),
        appWidgetUpdater: AppWidgetUpdater = AppWidgetUpdaterStub,
        currentEpochSecondsProvider: CurrentEpochSecondsProvider = SystemEpochSecondsProvider,
        block: (AddEditRecordViewModel) -> Unit
    ) {
        createViewModel(recordDao, appWidgetUpdater, currentEpochSecondsProvider).use(block)
    }

    private fun createRecord(expression: String): Record {
        return Record(
            id = 0,
            expression = expression,
            rawMeaning = "C123",
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
            listOf(
                createRecord("Expr1"),
                createRecord("Expr2"),
                createRecord("Expr3")
            )
        )

        useViewModel(recordDao = dao) { vm ->
            vm.expression = "Expr2"

            assertExpressionInvalid(vm, AddEditRecordMessage.EXISTING_EXPRESSION)
        }
    }

    @Test
    fun expressionIsValidWhenDbDoesNotContainsItTest() = runTest {
        val dao = db.recordDao()
        dao.insertAll(
            listOf(
                createRecord("Expr1"),
                createRecord("Expr2"),
                createRecord("Expr3")
            )
        )

        useViewModel(recordDao = dao) { vm ->
            vm.expression = "Expr4"

            assertExpressionValid(vm)
        }
    }

    @Test
    fun expressionIsValidWhenItEqualsToCurrentRecordExpressionTest() = runTest {
        val dao = db.recordDao()
        dao.insertAll(
            listOf(
                createRecord("Expr1"),
                createRecord("Expr2"),
                createRecord("Expr3")
            )
        )

        useViewModel(recordDao = dao) { vm ->
            val expectedCurrentRecord = dao.getRecordByExpression("Expr2")!!

            vm.currentRecordId = expectedCurrentRecord.id

            val actualCurrentRecord = vm.currentRecordFlow.filterNotNull().first()

            assertEquals(expectedCurrentRecord, actualCurrentRecord.getOrThrow())

            vm.expression = "Expr2"

            assertExpressionValid(vm)
        }
    }

    @Test
    fun currentRecordTest() = runTest {
        val dao = db.recordDao()
        dao.insertAll(
            listOf(
                createRecord("Expr1"),
                createRecord("Expr2"),
                createRecord("Expr3")
            )
        )

        val expectedRecord = dao.getRecordById(1)

        useViewModel(recordDao = dao) { vm ->
            vm.currentRecordId = 1

            val actualRecord = vm.currentRecordFlow.filterNotNull().first().getOrThrow()

            assertEquals(expectedRecord, actualRecord)
        }
    }

    @Test
    fun addRecordTest() = runTest {
        val dao = db.recordDao()
        val vm = createViewModel(recordDao = dao)

        vm.expression = "Expr1"
        vm.additionalNotes = "Notes1"
        vm.getMeaning = { ComplexMeaning.Common("Meaning") }

        vm.addOrEditExpression()

        vm.onAddError.handler = {
            assertTrue(false)
        }

        vm.onRecordSuccessfullyAdded.setHandlerAndWait {
            launch {
                val actualRecord = dao.getRecordByExpression("Expr1")!!

                assertEquals("CMeaning", actualRecord.rawMeaning)
                assertEquals("Notes1", actualRecord.additionalNotes)
                assertEquals(0, actualRecord.score)

                vm.clearThroughReflection()
            }
        }
    }

    @Test
    fun editRecordTest() = runTest {
        val dao = db.recordDao()
        val vm = createViewModel(recordDao = dao)

        dao.insert(
            Record(
                0,
                "Expr1_Old",
                "CMeaning1_Old",
                "Notes1_Old",
                1, 0
            )
        )

        vm.currentRecordId = 1

        // Wait until current record is loaded
        vm.currentRecordFlow.filterNotNull().first()

        vm.expression = "Expr1_New"
        vm.additionalNotes = "Notes1_New"
        vm.getMeaning = { ComplexMeaning.Common("Meaning1_New") }

        vm.addOrEditExpression()

        vm.onAddError.handler = {
            // Crash
            assertTrue(false)
        }

        vm.onRecordSuccessfullyAdded.setHandlerAndWait {
            launch {
                val actualRecord = dao.getRecordById(1)!!

                assertEquals("Expr1_New", actualRecord.expression)
                assertEquals("CMeaning1_New", actualRecord.rawMeaning)
                assertEquals("Notes1_New", actualRecord.additionalNotes)
                assertEquals(1, actualRecord.score)

                vm.clearThroughReflection()
            }
        }
    }

    @Test
    fun onRecordSuccessfullyAddedCalledOnMainThreadTest() = runTest {
        val vm = createViewModel()
        vm.getMeaning = { ComplexMeaning.Common("") }

        assertEventHandlerOnMainThread(vm, vm.onRecordSuccessfullyAdded) { addOrEditExpression() }
    }

    @Test
    fun onAddErrorCalledOnMainThreadTest() = runTest {
        val vm = createViewModel(recordDao = object : RecordDaoStub() {
            override suspend fun insert(value: Record) {
                throw RuntimeException()
            }
        })
        vm.getMeaning = { ComplexMeaning.Common("") }

        assertEventHandlerOnMainThread(vm, vm.onAddError) { addOrEditExpression() }
    }

    @Test
    fun updateAllAppWidgetsCalledOnMainThreadTest() = runTest {
        assertAppWidgetUpdateCalledOnMainThread(
            createVm = { updater ->
                createViewModel(appWidgetUpdater = updater).also { vm ->
                    vm.getMeaning = { ComplexMeaning.Common("") }
                }
            },
            triggerAction = { addOrEditExpression() }
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