package io.github.pelmenstar1.digiDict

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.pelmenstar1.digiDict.data.AppDatabase
import io.github.pelmenstar1.digiDict.data.RemoteDictionaryProviderStats
import io.github.pelmenstar1.digiDict.utils.AppDatabaseUtils
import io.github.pelmenstar1.digiDict.utils.reset
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.BeforeClass
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.test.assertEquals

@RunWith(AndroidJUnit4::class)
class RemoteDictionaryProviderStatsDaoTests {
    @Before
    fun before() {
        db.reset()
    }

    @Test
    fun updateIncrementVisitCountTest() = runTest {
        val dao = db.remoteDictionaryProviderStatsDao()

        dao.insert(RemoteDictionaryProviderStats(0, 1))
        dao.updateIncrementVisitCount(0)

        val actual = dao.getById(0)!!

        assertEquals(2, actual.visitCount)
    }

    @Test
    fun incrementVisitCountOnNonExistentEntry() = runTest {
        val dao = db.remoteDictionaryProviderStatsDao()

        // Ensure that the table is empty.
        db.reset()

        dao.incrementVisitCount(0)

        val actual = dao.getById(0)!!

        assertEquals(1, actual.visitCount)
    }

    companion object {
        private lateinit var db: AppDatabase

        @BeforeClass
        @JvmStatic
        fun beforeClass() {
            val context = ApplicationProvider.getApplicationContext<Context>()

            db = AppDatabaseUtils.createTestDatabase(context)
        }

        @After
        fun after() {
            db.close()
        }
    }
}