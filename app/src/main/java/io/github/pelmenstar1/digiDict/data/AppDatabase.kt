@file:Suppress("ClassName")

package io.github.pelmenstar1.digiDict.data

import android.content.Context
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ViewModel
import androidx.room.*
import androidx.room.migration.AutoMigrationSpec
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import io.github.pelmenstar1.digiDict.utils.getLazyValue

@Database(
    entities = [Record::class],
    exportSchema = true,
    version = 2,
    autoMigrations = [
        AutoMigration(
            from = 1,
            to = 2,
            spec = AppDatabase.Migration_1_2::class
        )
    ]
)
abstract class AppDatabase : RoomDatabase() {
    @DeleteColumn(tableName = "records", columnName = "origin")
    class Migration_1_2: AutoMigrationSpec

    abstract fun recordDao(): RecordDao

    inline fun createRecordTableObserver(crossinline block: () -> Unit): InvalidationTracker.Observer {
        return object : InvalidationTracker.Observer(RECORD_TABLE_ARRAY) {
            override fun onInvalidated(tables: MutableSet<String>) {
                block()
            }
        }
    }

    inline fun addRecordTableObserver(vm: ViewModel, crossinline block: () -> Unit) {
        val observer = createRecordTableObserver(block)

        val tracker = invalidationTracker.also {
            it.addObserver(observer)
        }

        vm.addCloseable {
            tracker.removeObserver(observer)
        }
    }

    inline fun addRecordTableObserver(lifecycle: Lifecycle, crossinline block: () -> Unit) {
        val tableObserver = createRecordTableObserver(block)

        val tracker = invalidationTracker.also {
            it.addObserver(tableObserver)
        }

        lifecycle.addObserver(object: DefaultLifecycleObserver {
            override fun onDestroy(owner: LifecycleOwner) {
                tracker.removeObserver(tableObserver)

                lifecycle.removeObserver(this)
            }
        })
    }
    companion object {
        val RECORD_TABLE_ARRAY = arrayOf(RecordTable.name)

        private var singleton: AppDatabase? = null

        fun getOrCreate(context: Context): AppDatabase {
            return synchronized(this) {
                getLazyValue(
                    singleton,
                    { createDatabase(context) }
                ) { singleton = it }
            }
        }

        fun createDatabase(context: Context): AppDatabase {
            return Room.databaseBuilder(context, AppDatabase::class.java, "database").build()
        }
    }
}