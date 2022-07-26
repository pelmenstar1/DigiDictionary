@file:Suppress("ClassName")

package io.github.pelmenstar1.digiDict.data

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.room.*
import androidx.room.migration.AutoMigrationSpec
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import io.github.pelmenstar1.digiDict.utils.getLazyValue

@Database(
    entities = [Record::class, RemoteDictionaryProviderInfo::class, RemoteDictionaryProviderStats::class],
    exportSchema = true,
    version = 4,
    autoMigrations = [
        AutoMigration(
            from = 1,
            to = 2,
            spec = AppDatabase.Migration_1_2::class
        ),
    ]
)
abstract class AppDatabase : RoomDatabase() {
    @DeleteColumn(tableName = "records", columnName = "origin")
    class Migration_1_2 : AutoMigrationSpec

    private object Migration_2_3 : Migration(2, 3) {
        override fun migrate(database: SupportSQLiteDatabase) {
            database.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_records_expression ON records(expression)")
        }
    }

    private object Migration_3_4 : Migration(3, 4) {
        override fun migrate(database: SupportSQLiteDatabase) {
            database.execSQL("CREATE TABLE IF NOT EXISTS remote_dict_providers (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, name TEXT NOT NULL, schema TEXT NOT NULL)")
            database.execSQL("CREATE TABLE IF NOT EXISTS `remote_dict_provider_stats` (`id` INTEGER NOT NULL, `visitCount` INTEGER NOT NULL, PRIMARY KEY(`id`))")

            RemoteDictionaryProviderInfo.PREDEFINED_PROVIDERS.forEach {
                database.insertRemoteDictProvider(it)
            }
        }
    }

    abstract fun recordDao(): RecordDao
    abstract fun remoteDictionaryProviderDao(): RemoteDictionaryProviderDao
    abstract fun remoteDictionaryProviderStatsDao(): RemoteDictionaryProviderStatsDao

    fun addRecordTableObserver(vm: ViewModel, block: () -> Unit) {
        addTableObserver(RECORD_TABLE_ARRAY, vm, block)
    }

    fun addRemoteDictProvidersTableObserver(vm: ViewModel, block: () -> Unit) {
        addTableObserver(REMOTE_DICT_PROVIDERS_ARRAY, vm, block)
    }

    private fun addTableObserver(tableNames: Array<out String>, vm: ViewModel, block: () -> Unit) {
        val observer = object : InvalidationTracker.Observer(tableNames) {
            override fun onInvalidated(tables: MutableSet<String>) {
                block()
            }
        }

        val tracker = invalidationTracker.also {
            it.addObserver(observer)
        }

        vm.addCloseable {
            tracker.removeObserver(observer)
        }
    }

    companion object {
        val RECORD_TABLE_ARRAY = arrayOf(RecordTable.name)
        val REMOTE_DICT_PROVIDERS_ARRAY = arrayOf("remote_dict_providers")

        private var singleton: AppDatabase? = null

        private fun SupportSQLiteDatabase.insertRemoteDictProvider(info: RemoteDictionaryProviderInfo) {
            execSQL("INSERT INTO remote_dict_providers (name, schema) VALUES('${info.name}', '${info.schema}')")
        }

        fun getOrCreate(context: Context): AppDatabase {
            return synchronized(this) {
                getLazyValue(
                    singleton,
                    { createDatabase(context) }
                ) { singleton = it }
            }
        }

        fun createDatabase(context: Context): AppDatabase {
            return Room
                .databaseBuilder(context, AppDatabase::class.java, "database")
                .addMigrations(Migration_2_3, Migration_3_4)
                .addCallback(object : Callback() {
                    override fun onCreate(db: SupportSQLiteDatabase) {
                        RemoteDictionaryProviderInfo.PREDEFINED_PROVIDERS.forEach {
                            db.insertRemoteDictProvider(it)
                        }
                    }
                })
                .build()
        }
    }
}