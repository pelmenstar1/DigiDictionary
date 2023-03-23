package io.github.pelmenstar1.digiDict.common

import android.content.Context
import androidx.arch.core.internal.SafeIterableMap
import androidx.lifecycle.ViewModel
import androidx.room.*
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import io.github.pelmenstar1.digiDict.common.android.onDatabaseTablesUpdated
import io.github.pelmenstar1.digiDict.common.android.queryArrayWithProgressReporter
import io.github.pelmenstar1.digiDict.commonTestUtils.clearThroughReflection
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
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
            Thread.sleep(200)
            assertTrue(isCbInvoked)
        }
    }

    @Test
    fun onDatabaseUpdatedShouldBeRemovedWhenViewModelClearedTest() {
        useDatabase { db ->
            val vm = TestViewModel()

            vm.onDatabaseTablesUpdated(db, arrayOf("test_entities")) {}
            vm.clearThroughReflection()

            val map = getObserverMap(db.invalidationTracker)

            assertEquals(0, map.size())
        }
    }

    @Suppress("UNCHECKED_CAST")
    private fun getObserverMap(tracker: InvalidationTracker): SafeIterableMap<InvalidationTracker.Observer, *> {
        // There is no public API to validate whether the observer is registered on InvalidationTracker.
        // In the currently used version of Room, InvalidationTracker has observerMap field that is used to store
        // Invalidation.Observer instances.
        val field = InvalidationTracker::class.java.getDeclaredField("observerMap")
        field.isAccessible = true

        return field.get(tracker) as SafeIterableMap<InvalidationTracker.Observer, *>
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
}