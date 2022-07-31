package io.github.pelmenstar1.digiDict.ui

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.pelmenstar1.digiDict.data.AppDatabase
import io.github.pelmenstar1.digiDict.data.Record
import io.github.pelmenstar1.digiDict.data.RecordDao
import io.github.pelmenstar1.digiDict.ui.viewRecord.ViewRecordViewModel
import io.github.pelmenstar1.digiDict.utils.*
import io.github.pelmenstar1.digiDict.widgets.AppWidgetUpdater
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
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
import kotlin.test.assertNull
import kotlin.test.assertTrue

@RunWith(AndroidJUnit4::class)
class ViewRecordViewModelTests {
    @Before
    fun before() {
        db.reset()
    }

    private fun createViewModel(
        dao: RecordDao = db.recordDao(),
        listAppWidgetUpdater: AppWidgetUpdater = AppWidgetUpdaterStub
    ): ViewRecordViewModel {
        return ViewRecordViewModel(dao, listAppWidgetUpdater)
    }

    @Test
    fun deleteTest() = runTest {
        val dao = db.recordDao()

        dao.insert(Record(0, "Expr1", "CMeaning1", "", 0, 0))
        val expectedToDeleteRecord = dao.getRecordByExpression("Expr1")!!

        val vm = createViewModel()

        vm.id = expectedToDeleteRecord.id
        vm.delete()

        vm.onRecordDeleted.setHandlerAndWait {
            launch {
                val deletedRecord = dao.getRecordById(expectedToDeleteRecord.id)

                assertNull(deletedRecord)

                vm.clearThroughReflection()
            }
        }

        vm.onDeleteError.handler = {
            assertTrue(false)
        }
    }

    @Test
    fun refreshRecordTest() = runTest {
        val expectedId = 4
        val expectedRecord = Record(expectedId, "Expr1", "CMeaning1", "AdditionalNotes", 1, 2)
        val vm = createViewModel(dao = object : RecordDaoStub() {
            override fun getRecordFlowById(id: Int): Flow<Record?> {
                return emptyFlow()
            }

            override suspend fun getRecordById(id: Int): Record {
                assertEquals(expectedId, id)

                return expectedRecord
            }
        })

        vm.id = expectedId

        vm.refreshRecord()

        val actualRecord = vm.recordFlow?.filterNotNull()?.first()
        assertEquals(expectedRecord, actualRecord)

    }

    @Test
    fun onDeleteErrorCalledOnMainThreadTest() = runTest {
        val vm = createViewModel(dao = object : RecordDaoStub() {
            override suspend fun deleteById(id: Int): Int {
                throw RuntimeException()
            }
        })

        assertEventHandlerOnMainThread(vm, vm.onDeleteError) { delete() }
    }

    @Test
    fun onRefreshErrorCalledOnMainThreadTest() = runTest {
        val vm = createViewModel(dao = object : RecordDaoStub() {
            override suspend fun getRecordById(id: Int): Record? {
                throw RuntimeException()
            }
        })

        assertEventHandlerOnMainThread(vm, vm.onRefreshError) { refreshRecord() }
    }

    @Test
    fun onRecordDeletedCalledOnMainThreadTest() = runTest {
        val vm = createViewModel()

        vm.use {
            assertEventHandlerOnMainThread(vm, vm.onRecordDeleted) { delete() }
        }
    }

    @Test
    fun updateAllWidgetsCalledOnMainThreadTest() = runTest {
        assertAppWidgetUpdateCalledOnMainThread(
            createVm = { updater -> createViewModel(listAppWidgetUpdater = updater) },
            triggerAction = { delete() }
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