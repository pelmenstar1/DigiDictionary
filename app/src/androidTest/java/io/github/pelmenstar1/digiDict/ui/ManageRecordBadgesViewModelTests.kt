package io.github.pelmenstar1.digiDict.ui

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.pelmenstar1.digiDict.data.*
import io.github.pelmenstar1.digiDict.ui.manageRecordBadges.ManageRecordBadgesViewModel
import io.github.pelmenstar1.digiDict.utils.AppDatabaseUtils
import io.github.pelmenstar1.digiDict.utils.reset
import io.github.pelmenstar1.digiDict.utils.runAndWaitForResult
import io.github.pelmenstar1.digiDict.utils.use
import kotlinx.coroutines.test.runTest
import org.junit.AfterClass
import org.junit.Before
import org.junit.BeforeClass
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.test.assertEquals
import kotlin.test.assertNull

@RunWith(AndroidJUnit4::class)
class ManageRecordBadgesViewModelTests {
    @Before
    fun before() {
        db.reset()
    }

    private inline fun useViewModel(
        recordBadgeDao: RecordBadgeDao = db.recordBadgeDao(),
        recordToBadgeRelationDao: RecordToBadgeRelationDao = db.recordToBadgeRelationDao(),
        block: (ManageRecordBadgesViewModel) -> Unit
    ) {
        ManageRecordBadgesViewModel(recordToBadgeRelationDao, recordBadgeDao).use(block)
    }


    @Test
    fun removeBadgeTest() = runTest {
        val recordBadgeDao = db.recordBadgeDao()
        val recordToBadgeRelationDao = db.recordToBadgeRelationDao()

        val badge = RecordBadgeInfo(1, "Badge", 3)
        recordBadgeDao.insert(badge)
        recordToBadgeRelationDao.insertAll(
            arrayOf(
                RecordToBadgeRelation(0, recordId = 0, badge.id),
                RecordToBadgeRelation(0, recordId = 1, badge.id)
            )
        )

        useViewModel { vm ->
            vm.removeAction.runAndWaitForResult(badge)

            val currentBadge = recordBadgeDao.getById(badge.id)
            val relations = recordToBadgeRelationDao.getByBadgeId(badge.id)

            assertNull(currentBadge)
            assertEquals(0, relations.size)
        }
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