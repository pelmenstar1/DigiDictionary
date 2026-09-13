package io.github.pelmenstar1.digiDict.common

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.PrimaryKey
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import io.github.pelmenstar1.digiDict.common.android.onDatabaseTablesUpdated
import io.github.pelmenstar1.digiDict.common.android.queryArrayWithProgressReporter
import io.github.pelmenstar1.digiDict.commonTestUtils.clear
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.test.assertContentEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@RunWith(AndroidJUnit4::class)
class DatabaseKtTests {
    @Entity(tableName = "test_entities")
    data class TestEntity(@PrimaryKey val id: Int, val value: String)

    @Dao
    interface TestEntityDao {
        @Insert
        fun insert(value: TestEntity)
    }

    @Database(entities = [TestEntity::class], version = 1, exportSchema = false)
    abstract class TestDatabase : RoomDatabase() {
        abstract fun testEntityDao(): TestEntityDao

        companion object {
            fun createInMemory(context: Context) = Room
                .inMemoryDatabaseBuilder(context, TestDatabase::class.java)
                .build()
        }
    }

    private class TestViewModel : ViewModel()

    private val context = InstrumentationRegistry.getInstrumentation().context

    private inline fun useDatabase(block: (TestDatabase) -> Unit) {
        val db = TestDatabase.createInMemory(context)

        try {
            block(db)
        } finally {
            db.close()
        }
    }

    @Test
    fun onDatabaseTablesUpdatedTest() {
        useDatabase { db ->
            val vm = TestViewModel()
            var isCbInvoked = false

            vm.onDatabaseTablesUpdated(db, arrayOf("test_entities")) {
                isCbInvoked = true
            }

            db.testEntityDao().insert(TestEntity(id = 1, value = "123"))

            // Changes are pulled on another thread - we need to wait some time.
            Thread.sleep(TABLE_UPDATE_TIMEOUT_MS)
            assertTrue(isCbInvoked)
        }
    }

    @Test
    fun onDatabaseUpdatedShouldBeRemovedWhenViewModelClearedTest() {
        useDatabase { db ->
            val dao = db.testEntityDao()
            val vm = TestViewModel()
            var isCbInvoked = false

            vm.onDatabaseTablesUpdated(db, arrayOf("test_entities")) {
                isCbInvoked = true
            }

            dao.insert(TestEntity(id = 1, value = "123"))
            Thread.sleep(TABLE_UPDATE_TIMEOUT_MS)
            assertTrue(isCbInvoked, "callback is expected to be invoked while the view-model is alive")

            isCbInvoked = false
            vm.clear()

            dao.insert(TestEntity(id = 2, value = "456"))
            Thread.sleep(TABLE_UPDATE_TIMEOUT_MS)
            assertFalse(isCbInvoked, "callback is not expected to be invoked after the view-model is cleared")
        }
    }

    @Test
    fun queryArrayWithProgressReporterTest() {
        fun testCase(entities: Array<TestEntity>) = runBlocking {
            useDatabase { db ->
                val dao = db.testEntityDao()
                entities.forEach(dao::insert)

                val actualEntities = db.queryArrayWithProgressReporter(
                    sql = "SELECT * FROM test_entities",
                    progressReporter = null
                ) { c ->
                    val id = c.getInt(0)
                    val value = c.getString(1)

                    TestEntity(id, value)
                }

                assertContentEquals(entities, actualEntities)
            }
        }

        testCase(emptyArray())
        testCase(arrayOf(TestEntity(id = 1, value = "A")))
        testCase(
            arrayOf(
                TestEntity(id = 1, value = "A"),
                TestEntity(id = 2, value = "B"),
                TestEntity(id = 3, value = "C")
            ),
        )
    }

    companion object {
        // Table changes are pulled on another thread, so the assertions have to wait for it.
        private const val TABLE_UPDATE_TIMEOUT_MS = 200L
    }
}