package io.github.pelmenstar1.digiDict.ui

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.pelmenstar1.digiDict.common.DataLoadState
import io.github.pelmenstar1.digiDict.common.firstSuccess
import io.github.pelmenstar1.digiDict.data.AppDatabase
import io.github.pelmenstar1.digiDict.data.Record
import io.github.pelmenstar1.digiDict.data.RecordDao
import io.github.pelmenstar1.digiDict.data.SearchPreparedRecordDao
import io.github.pelmenstar1.digiDict.ui.viewRecord.ViewRecordViewModel
import io.github.pelmenstar1.digiDict.utils.*
import io.github.pelmenstar1.digiDict.widgets.AppWidgetUpdater
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import org.junit.AfterClass
import org.junit.Before
import org.junit.BeforeClass
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.fail

@RunWith(AndroidJUnit4::class)
class ViewRecordViewModelTests {
    @Before
    fun before() {
        db.reset()
    }

    private fun createViewModel(
        recordDao: RecordDao = db.recordDao(),
        searchPreparedRecordDao: SearchPreparedRecordDao = db.searchPreparedRecordDao(),
        listAppWidgetUpdater: AppWidgetUpdater = AppWidgetUpdaterStub
    ): ViewRecordViewModel {
        return ViewRecordViewModel(recordDao, searchPreparedRecordDao, listAppWidgetUpdater)
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
            fail()
        }
    }

    @Test
    fun loadRecordTest() = runTest {
        val expectedId = 4
        val expectedRecord = Record(expectedId, "Expr1", "CMeaning1", "AdditionalNotes", 1, 2)
        val vm = createViewModel(recordDao = object : RecordDaoStub() {
            override fun getRecordFlowById(id: Int): Flow<Record?> {
                assertEquals(expectedId, id)

                return flowOf(expectedRecord)
            }
        })

        vm.id = expectedId

        val actualRecord = vm.dataStateFlow.filterIsInstance<DataLoadState.Success<Record?>>().first().value

        assertEquals(expectedRecord, actualRecord)
    }

    @Test
    fun refreshRecordTest() = runTest {
        val expectedId = 4
        val expectedRecord = Record(expectedId, "Expr1", "CMeaning1", "AdditionalNotes", 1, 2)

        var isFirstCall = true
        val vm = createViewModel(recordDao = object : RecordDaoStub() {
            override fun getRecordFlowById(id: Int): Flow<Record?> {
                assertEquals(expectedId, id)

                return if (isFirstCall) {
                    isFirstCall = false

                    flow { throw RuntimeException() }
                } else {
                    flowOf(expectedRecord)
                }
            }
        })

        vm.id = expectedId

        // There should be error state, if there are not, test will time out.
        vm.dataStateFlow.filterIsInstance<DataLoadState.Error<Record?>>().first()

        vm.retryLoadData()

        val actualRecord = vm.dataStateFlow.firstSuccess()
        assertEquals(expectedRecord, actualRecord)
    }

    @Test
    fun onDeleteErrorCalledOnMainThreadTest() = runTest {
        val vm = createViewModel(recordDao = object : RecordDaoStub() {
            override suspend fun deleteById(id: Int): Int {
                throw RuntimeException()
            }
        })

        assertEventHandlerOnMainThread(vm, vm.onDeleteError) { delete() }
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