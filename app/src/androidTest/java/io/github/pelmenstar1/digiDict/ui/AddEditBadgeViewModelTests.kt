package io.github.pelmenstar1.digiDict.ui

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import io.github.pelmenstar1.digiDict.common.firstSuccess
import io.github.pelmenstar1.digiDict.commonTestUtils.runAndWaitForResult
import io.github.pelmenstar1.digiDict.commonTestUtils.use
import io.github.pelmenstar1.digiDict.data.AppDatabase
import io.github.pelmenstar1.digiDict.data.RecordBadgeDao
import io.github.pelmenstar1.digiDict.data.RecordBadgeInfo
import io.github.pelmenstar1.digiDict.ui.addEditBadge.AddEditBadgeFragmentMessage
import io.github.pelmenstar1.digiDict.ui.addEditBadge.AddEditBadgeViewModel
import io.github.pelmenstar1.digiDict.utils.AppDatabaseUtils
import io.github.pelmenstar1.digiDict.utils.reset
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.AfterClass
import org.junit.Before
import org.junit.BeforeClass
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class AddEditBadgeViewModelTests {
    @Before
    fun before() {
        db.reset()
    }

    private inline fun useViewModel(
        badgeDao: RecordBadgeDao = db.recordBadgeDao(),
        block: (AddEditBadgeViewModel) -> Unit
    ) {
        AddEditBadgeViewModel(badgeDao).use(block)
    }

    @Test
    fun nameErrorIsEmptyWhenInputIsEmpty() = runTest {
        useViewModel { vm ->
            vm.currentBadgeId = -1
            vm.name = ""

            // Checking name is async, so we need to wait some time
            Thread.sleep(200)

            assertEquals(AddEditBadgeFragmentMessage.EMPTY_TEXT, vm.nameErrorFlow.first())
        }
    }

    @Test
    fun noNameErrorWhenNoBadges() = runTest {
        db.reset()

        suspend fun testCase(name: String) {
            useViewModel { vm ->
                vm.currentBadgeId = -1
                vm.name = name

                // Checking name is async, so we need to wait some time
                Thread.sleep(100)

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
                vm.currentBadgeId = -1
                vm.name = name

                // Checking name is async, so we need to wait some time
                Thread.sleep(100)

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
        val badgeName = "Badge"

        badgeDao.insert(RecordBadgeInfo(id = 0, name = badgeName, outlineColor = 2))
        val badgeId = badgeDao.getIdByName(badgeName)!!

        useViewModel { vm ->
            vm.currentBadgeId = badgeId
            vm.name = badgeName

            // Checking name is async, so we need to wait some time
            Thread.sleep(100)

            assertNull(vm.nameErrorFlow.first())
        }
    }

    @Test
    fun nameErrorIsExistsWhenInputIsExistentBadgeName() = runTest {
        val badgeDao = db.recordBadgeDao()
        val badge = RecordBadgeInfo(id = 0, name = "Badge", outlineColor = 3)
        badgeDao.insert(badge)

        useViewModel { vm ->
            vm.currentBadgeId = -1
            vm.name = badge.name

            // Checking name is async, so we need to wait some time
            Thread.sleep(100)

            assertEquals(AddEditBadgeFragmentMessage.NAME_EXISTS, vm.nameErrorFlow.first())
        }
    }

    @Test
    fun editTest() = runTest {
        val recordBadgeDao = db.recordBadgeDao()

        useViewModel { vm ->
            val oldName = "Old"
            val newName = "New"
            val newOutlineColor = 2

            recordBadgeDao.insert(RecordBadgeInfo(0, oldName, 1))

            val badgeId = recordBadgeDao.getIdByName(oldName)!!
            vm.currentBadgeId = badgeId

            // TODO: Create and use a helper to throw an exception if error in state flow happens
            vm.currentBadgeStateFlow.firstSuccess()

            vm.name = newName
            vm.outlineColor = newOutlineColor

            vm.addOrEditAction.runAndWaitForResult()

            val actualBadge = recordBadgeDao.getById(badgeId)

            assertNotNull(actualBadge)
            assertEquals(newName, actualBadge.name)
            assertEquals(newOutlineColor, actualBadge.outlineColor)
        }
    }

    @Test
    fun addTest() = runTest {
        val recordBadgeDao = db.recordBadgeDao()

        useViewModel { vm ->
            val name = "Badge"
            val outlineColor = 2

            vm.currentBadgeId = -1
            vm.name = name
            vm.outlineColor = outlineColor

            vm.addOrEditAction.runAndWaitForResult()

            val actualBadge = recordBadgeDao.getByName(name)

            assertNotNull(actualBadge)
            assertEquals(name, actualBadge.name)
            assertEquals(outlineColor, actualBadge.outlineColor)
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