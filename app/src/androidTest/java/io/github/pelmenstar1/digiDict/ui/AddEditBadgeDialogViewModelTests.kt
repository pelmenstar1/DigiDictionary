package io.github.pelmenstar1.digiDict.ui

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import io.github.pelmenstar1.digiDict.data.AppDatabase
import io.github.pelmenstar1.digiDict.data.RecordBadgeDao
import io.github.pelmenstar1.digiDict.data.RecordBadgeInfo
import io.github.pelmenstar1.digiDict.ui.manageRecordBadges.AddEditBadgeDialogViewModel
import io.github.pelmenstar1.digiDict.ui.manageRecordBadges.AddEditBadgeInputMessage
import io.github.pelmenstar1.digiDict.utils.AppDatabaseUtils
import io.github.pelmenstar1.digiDict.utils.reset
import io.github.pelmenstar1.digiDict.utils.use
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.AfterClass
import org.junit.Before
import org.junit.BeforeClass
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class AddEditBadgeDialogViewModelTests {
    @Before
    fun before() {
        db.reset()
    }

    private inline fun useViewModel(
        badgeDao: RecordBadgeDao = db.recordBadgeDao(),
        block: (AddEditBadgeDialogViewModel) -> Unit
    ) {
        AddEditBadgeDialogViewModel(badgeDao).use(block)
    }


    @Test
    fun nameErrorIsEmptyWhenInputIsEmpty() = runTest {
        useViewModel { vm ->
            vm.name = ""

            assertEquals(AddEditBadgeInputMessage.EMPTY_TEXT, vm.nameErrorFlow.first())
        }
    }

    @Test
    fun noNameErrorWhenNoBadges() = runTest {
        suspend fun testCase(name: String) {
            useViewModel { vm ->
                vm.name = name

                assertNull(vm.nameErrorFlow.first())
            }
        }

        testCase("A")
        testCase("ABC")
        testCase("Badge")
    }

    @Test
    fun noNameErrorWhenThereAreSomeBadges() = runTest {
        val recordBadgeDao = db.recordBadgeDao()
        recordBadgeDao.insertAll(
            arrayOf(
                RecordBadgeInfo(id = 0, name = "B", outlineColor = 0),
                RecordBadgeInfo(id = 0, name = "BB", outlineColor = 0)
            )
        )

        suspend fun testCase(name: String) {
            useViewModel { vm ->
                vm.name = name

                assertNull(vm.nameErrorFlow.first())
            }
        }

        testCase("A")
        testCase("ABC")
        testCase("Badge")
    }

    @Test
    fun noNameErrorWhenInputIsCurrentBadgeName() = runTest {
        val badgeDao = db.recordBadgeDao()
        val badge = RecordBadgeInfo(id = 1, name = "Badge", outlineColor = 2)
        badgeDao.insert(badge)

        useViewModel { vm ->
            vm.currentBadgeName = badge.name
            vm.name = badge.name

            assertNull(vm.nameErrorFlow.first())
        }
    }

    @Test
    fun nameErrorIsExistsWhenInputIsExistentBadgeName() = runTest {
        val badgeDao = db.recordBadgeDao()
        val badge = RecordBadgeInfo(id = 0, name = "Badge", outlineColor = 3)
        badgeDao.insert(badge)

        useViewModel { vm ->
            vm.name = badge.name

            assertEquals(AddEditBadgeInputMessage.EXISTS, vm.nameErrorFlow.first())
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