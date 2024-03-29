package io.github.pelmenstar1.digiDict.ui

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.pelmenstar1.digiDict.common.DataLoadState
import io.github.pelmenstar1.digiDict.common.firstSuccess
import io.github.pelmenstar1.digiDict.commonTestUtils.clearThroughReflection
import io.github.pelmenstar1.digiDict.commonTestUtils.runAndWaitForResult
import io.github.pelmenstar1.digiDict.commonTestUtils.waitUntilSuccessOrThrowOnError
import io.github.pelmenstar1.digiDict.data.AppDatabase
import io.github.pelmenstar1.digiDict.data.Record
import io.github.pelmenstar1.digiDict.data.RecordBadgeInfo
import io.github.pelmenstar1.digiDict.data.RecordDao
import io.github.pelmenstar1.digiDict.ui.viewRecord.ViewRecordViewModel
import io.github.pelmenstar1.digiDict.utils.*
import io.github.pelmenstar1.digiDict.widgets.AppWidgetUpdater
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.runTest
import org.junit.AfterClass
import org.junit.Before
import org.junit.BeforeClass
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.test.assertEquals
import kotlin.test.assertNull

@RunWith(AndroidJUnit4::class)
class ViewRecordViewModelTests {
    @Before
    fun before() {
        db.reset()
    }

    private fun createViewModel(
        recordDao: RecordDao = db.recordDao(),
        listAppWidgetUpdater: AppWidgetUpdater = AppWidgetUpdaterStub
    ): ViewRecordViewModel {
        return ViewRecordViewModel(recordDao, listAppWidgetUpdater)
    }

    @Test
    fun deleteTest() = runTest {
        val dao = db.recordDao()

        dao.insert(Record(1, "Expr1", "CMeaning1", "", 0, 0))
        val expectedToDeleteRecord = dao.getRecordByExpression("Expr1")!!

        val vm = createViewModel()

        vm.id = expectedToDeleteRecord.id
        vm.deleteAction.runAndWaitForResult()
        val deletedRecord = dao.getRecordById(expectedToDeleteRecord.id)
        assertNull(deletedRecord)

        vm.clearThroughReflection()
    }

    @Test
    fun loadRecordTest() = runTest {
        suspend fun testCase(badges: Array<RecordBadgeInfo>) {
            val expectedRecordId = 4
            val expectedRecordWithBadges = db.addRecordAndBadges(
                record = Record(expectedRecordId, "Expr1", "CMeaning1", "AdditionalNotes", 1, 2),
                badges = badges
            )

            val vm = createViewModel()
            vm.id = expectedRecordId

            val actualRecord = vm.dataStateFlow.waitUntilSuccessOrThrowOnError()
            assertEquals(expectedRecordWithBadges, actualRecord)

            db.reset()
            vm.clearThroughReflection()
        }

        testCase(badges = emptyArray())
        testCase(badges = arrayOf(RecordBadgeInfo(1, "Badge1", 1)))
        testCase(
            badges = arrayOf(
                RecordBadgeInfo(1, "Badge1", 1),
                RecordBadgeInfo(2, "Badge2", 2)
            )
        )
    }

    @Test
    fun refreshRecordTest() = runTest(dispatchTimeoutMs = 10_000) {
        suspend fun testCase(badges: Array<RecordBadgeInfo>) {
            val expectedRecordId = 4
            val expectedRecordWithBadges = db.addRecordAndBadges(
                record = Record(expectedRecordId, "Expr1", "CMeaning1", "AdditionalNotes", 1, 2),
                badges = badges
            )

            val recordDao = db.recordDao()

            var isFirstCall = true
            val vm = createViewModel(recordDao = object : RecordDaoStub() {
                override fun getRecordBadgesFlowByRecordId(id: Int): Flow<Array<RecordBadgeInfo>> {
                    return recordDao.getRecordBadgesFlowByRecordId(id)
                }

                override suspend fun getRecordBadgesByRecordId(id: Int): Array<RecordBadgeInfo> {
                    return recordDao.getRecordBadgesByRecordId(id)
                }

                override fun getRecordFlowById(id: Int): Flow<Record?> {
                    return if (isFirstCall) {
                        isFirstCall = false

                        flow { throw RuntimeException() }
                    } else {
                        recordDao.getRecordFlowById(id)
                    }
                }
            })

            vm.id = expectedRecordId

            // There should be error state, if there are not, test will time out.
            vm.dataStateFlow.filterIsInstance<DataLoadState.Error<Record?>>().first()

            vm.retryLoadData()

            val actualRecord = vm.dataStateFlow.firstSuccess()
            assertEquals(expectedRecordWithBadges, actualRecord)

            db.reset()
            vm.clearThroughReflection()
        }

        testCase(badges = emptyArray())
        testCase(badges = arrayOf(RecordBadgeInfo(1, "Badge1", 1)))
        testCase(
            badges = arrayOf(
                RecordBadgeInfo(1, "Badge1", 1),
                RecordBadgeInfo(2, "Badge2", 2)
            )
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