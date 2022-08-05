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
    version = 5,
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

    object Migration_2_3 : Migration(2, 3) {
        override fun migrate(database: SupportSQLiteDatabase) {
            database.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_records_expression ON records(expression)")
        }
    }

    object Migration_3_4 : Migration(3, 4) {
        override fun migrate(database: SupportSQLiteDatabase) {
            database.runInTransitionBlocking {
                execSQL("CREATE TABLE IF NOT EXISTS remote_dict_providers (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, name TEXT NOT NULL, schema TEXT NOT NULL)")
                execSQL("CREATE TABLE IF NOT EXISTS `remote_dict_provider_stats` (`id` INTEGER NOT NULL, `visitCount` INTEGER NOT NULL, PRIMARY KEY(`id`))")

                insertRemoteDictProviders_4(RemoteDictionaryProviderInfo.PREDEFINED_PROVIDERS)
            }
        }
    }

    object Migration_4_5 : Migration(4, 5) {
        override fun migrate(database: SupportSQLiteDatabase) {
            database.runInTransitionBlocking {
                execSQL("DROP TABLE remote_dict_providers")
                execSQL("DROP TABLE remote_dict_provider_stats")
                execSQL("CREATE TABLE remote_dict_providers (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, name TEXT NOT NULL, schema TEXT NOT NULL, urlEncodingRules TEXT NOT NULL)")
                execSQL("CREATE TABLE remote_dict_provider_stats (`id` INTEGER NOT NULL, `visitCount` INTEGER NOT NULL, PRIMARY KEY(`id`))")

                insertRemoteDictProviders_5(RemoteDictionaryProviderInfo.PREDEFINED_PROVIDERS)
            }
        }
    }

    abstract fun recordDao(): RecordDao
    abstract fun remoteDictionaryProviderDao(): RemoteDictionaryProviderDao
    abstract fun remoteDictionaryProviderStatsDao(): RemoteDictionaryProviderStatsDao

    companion object {
        private var singleton: AppDatabase? = null

        // Inserts given providers to the DB with version 4
        internal fun SupportSQLiteDatabase.insertRemoteDictProviders_4(providers: Array<out RemoteDictionaryProviderInfo>) {
            val statement = compileStatement("INSERT INTO remote_dict_providers (name, schema) VALUES(?, ?)")

            runInTransitionBlocking {
                providers.forEach {
                    // Binding is 1-based.
                    statement.bindString(1, it.name)
                    statement.bindString(2, it.schema)

                    statement.executeInsert()
                }
            }
        }

        // Inserts given providers to the DB with version 5
        internal fun SupportSQLiteDatabase.insertRemoteDictProviders_5(providers: Array<out RemoteDictionaryProviderInfo>) {
            val statement =
                compileStatement("INSERT INTO remote_dict_providers (name, schema, urlEncodingRules) VALUES(?, ?, ?)")

            runInTransitionBlocking {
                providers.forEach {
                    // Binding is 1-based.
                    statement.bindString(1, it.name)
                    statement.bindString(2, it.schema)
                    statement.bindString(3, it.urlEncodingRules.raw)

                    statement.executeInsert()
                }
            }
        }

        fun getOrCreate(context: Context): AppDatabase {
            return synchronized(this) {
                getLazyValue(
                    singleton,
                    { createFileDatabase(context) }
                ) { singleton = it }
            }
        }

        fun createInMemory(context: Context): AppDatabase {
            return Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).configureAndBuild()
        }

        private fun createFileDatabase(context: Context): AppDatabase {
            return Room
                .databaseBuilder(context, AppDatabase::class.java, "database")
                .configureAndBuild()
        }

        private fun Builder<AppDatabase>.configureAndBuild(): AppDatabase =
            this.addMigrations(Migration_2_3, Migration_3_4, Migration_4_5)
                .addCallback(object : Callback() {
                    override fun onCreate(db: SupportSQLiteDatabase) {
                        db.insertRemoteDictProviders_5(RemoteDictionaryProviderInfo.PREDEFINED_PROVIDERS)
                    }
                })
                .build()

        internal inline fun SupportSQLiteDatabase.runInTransitionBlocking(block: SupportSQLiteDatabase.() -> Unit) {
            beginTransaction()

            try {
                block()

                setTransactionSuccessful()
            } finally {
                endTransaction()
            }
        }
    }
}