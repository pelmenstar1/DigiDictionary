package io.github.pelmenstar1.digiDict.ui

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.pelmenstar1.digiDict.common.mapToIntArray
import io.github.pelmenstar1.digiDict.commonTestUtils.use
import io.github.pelmenstar1.digiDict.data.AppDatabase
import io.github.pelmenstar1.digiDict.data.RecordBadgeDao
import io.github.pelmenstar1.digiDict.data.RecordBadgeInfo
import io.github.pelmenstar1.digiDict.ui.addEditRecord.badge.BadgeSelectorDialogViewModel
import io.github.pelmenstar1.digiDict.utils.AppDatabaseUtils
import io.github.pelmenstar1.digiDict.utils.reset
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.AfterClass
import org.junit.Before
import org.junit.BeforeClass
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.test.assertContentEquals

@RunWith(AndroidJUnit4::class)
class BadgeSelectorDialogViewModelTests {
    @Before
    fun before() {
        db.reset()
    }

    private inline fun useViewModel(
        recordBadgeDao: RecordBadgeDao = db.recordBadgeDao(),
        block: (BadgeSelectorDialogViewModel) -> Unit
    ) {
        BadgeSelectorDialogViewModel(recordBadgeDao).use(block)
    }

    @Test
    fun validBadgesFlowTest() = runTest {
        suspend fun testCase(allIds: IntArray, usedIds: IntArray, expectedIds: IntArray) {
            val recordBadgeDao = db.recordBadgeDao()
            recordBadgeDao.insertAll(allIds.map { id ->
                RecordBadgeInfo(id, "Badge$id", 3)
            }.toTypedArray())

            useViewModel { vm ->
                vm.usedBadgeIds = usedIds

                val actualResult = vm.validBadgesFlow.first().toTypedArray()
                assertContentEquals(expectedIds, actualResult.mapToIntArray { it.id })
            }

            db.reset()
        }

        testCase(
            allIds = intArrayOf(1, 2, 3),
            usedIds = intArrayOf(1),
            expectedIds = intArrayOf(2, 3)
        )

        testCase(
            allIds = intArrayOf(1, 2, 3),
            usedIds = IntArray(0),
            expectedIds = intArrayOf(1, 2, 3)
        )

        testCase(
            allIds = intArrayOf(1, 2, 3),
            usedIds = intArrayOf(1, 2, 3),
            expectedIds = IntArray(0)
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