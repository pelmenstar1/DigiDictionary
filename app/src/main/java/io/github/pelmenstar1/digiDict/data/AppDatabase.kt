@file:Suppress("ClassName")

package io.github.pelmenstar1.digiDict.data

import android.content.Context
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
@TypeConverters(Converters::class)
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
            database.run {
                execSQL("CREATE TABLE IF NOT EXISTS remote_dict_providers (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, name TEXT NOT NULL, schema TEXT NOT NULL)")
                execSQL("CREATE TABLE IF NOT EXISTS `remote_dict_provider_stats` (`id` INTEGER NOT NULL, `visitCount` INTEGER NOT NULL, PRIMARY KEY(`id`))")

                insertRemoteDictProviders(RemoteDictionaryProviderInfo.PREDEFINED_PROVIDERS)
            }
        }
    }

    abstract fun recordDao(): RecordDao
    abstract fun remoteDictionaryProviderDao(): RemoteDictionaryProviderDao
    abstract fun remoteDictionaryProviderStatsDao(): RemoteDictionaryProviderStatsDao

    companion object {
        private var singleton: AppDatabase? = null

        internal fun SupportSQLiteDatabase.insertRemoteDictProviders(providers: Array<out RemoteDictionaryProviderInfo>) {
            val statement =
                compileStatement("INSERT INTO remote_dict_providers (name, schema, urlEncodingRules) VALUES(?, ?, ?)")

            beginTransaction()
            try {
                providers.forEach {
                    // Binding is 1-based.
                    statement.bindString(1, it.name)
                    statement.bindString(2, it.schema)
                    statement.bindString(3, it.urlEncodingRules.raw)

                    statement.executeInsert()
                }

                setTransactionSuccessful()
            } finally {
                endTransaction()
            }
        }

        fun getOrCreate(context: Context): AppDatabase {
            return synchronized(this) {
                getLazyValue(
                    singleton,
                    { createDatabase(context) }
                ) { singleton = it }
            }
        }

        private fun createDatabase(context: Context): AppDatabase {
            return Room
                .databaseBuilder(context, AppDatabase::class.java, "database")
                .addMigrations(Migration_2_3, Migration_3_4)
                .addCallback(object : Callback() {
                    override fun onCreate(db: SupportSQLiteDatabase) {
                        db.insertRemoteDictProviders(RemoteDictionaryProviderInfo.PREDEFINED_PROVIDERS)
                    }
                })
                .build()
        }
    }
}