package io.github.pelmenstar1.digiDict.data

import android.content.Context
import androidx.room.*
import androidx.room.migration.AutoMigrationSpec
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import io.github.pelmenstar1.digiDict.common.android.runInTransactionBlocking
import io.github.pelmenstar1.digiDict.common.getLazyValue

@Database(
    entities = [
        Record::class,
        RemoteDictionaryProviderInfo::class,
        RemoteDictionaryProviderStats::class,
        RecordBadgeInfo::class,
        RecordToBadgeRelation::class,
        EventInfo::class,
        WordQueueEntry::class
    ],
    exportSchema = true,
    version = 12,
    autoMigrations = [
        AutoMigration(
            from = 1,
            to = 2,
            spec = AppDatabase.Migration_1_2::class
        ),
        AutoMigration(
            from = 6,
            to = 7,
            spec = AppDatabase.Migration_6_7::class
        ),
        AutoMigration(
            from = 7,
            to = 8,
            spec = AppDatabase.Migration_7_8::class
        ),
        AutoMigration(
            from = 8,
            to = 9,
            spec = AppDatabase.Migration_8_9::class
        ),
        AutoMigration(
            from = 9,
            to = 10,
            spec = AppDatabase.Migration_9_10::class
        ),
        AutoMigration(
            from = 11,
            to = 12,
            spec = AppDatabase.Migration_11_12::class
        )
    ]
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    @DeleteColumn(tableName = "records", columnName = "origin")
    class Migration_1_2 : AutoMigrationSpec

    class Migration_6_7 : AutoMigrationSpec

    @DeleteTable(tableName = "search_prepared_records")
    class Migration_7_8 : AutoMigrationSpec

    class Migration_8_9 : AutoMigrationSpec

    class Migration_9_10 : AutoMigrationSpec

    class Migration_11_12 : AutoMigrationSpec

    object Migration_2_3 : Migration(2, 3) {
        override fun migrate(database: SupportSQLiteDatabase) {
            database.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_records_expression ON records(expression)")
        }
    }

    object Migration_3_4 : Migration(3, 4) {
        override fun migrate(database: SupportSQLiteDatabase) {
            database.runInTransactionBlocking {
                execSQL("CREATE TABLE IF NOT EXISTS remote_dict_providers (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, name TEXT NOT NULL, schema TEXT NOT NULL)")
                execSQL("CREATE TABLE IF NOT EXISTS `remote_dict_provider_stats` (`id` INTEGER NOT NULL, `visitCount` INTEGER NOT NULL, PRIMARY KEY(`id`))")

                insertRemoteDictProviders_4(RemoteDictionaryProviderInfo.PREDEFINED_PROVIDERS)
            }
        }
    }

    object Migration_4_5 : Migration(4, 5) {
        override fun migrate(database: SupportSQLiteDatabase) {
            database.runInTransactionBlocking {
                execSQL("DROP TABLE remote_dict_providers")
                execSQL("DROP TABLE remote_dict_provider_stats")
                execSQL("CREATE TABLE remote_dict_providers (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, name TEXT NOT NULL, schema TEXT NOT NULL, urlEncodingRules TEXT NOT NULL)")
                execSQL("CREATE TABLE remote_dict_provider_stats (`id` INTEGER NOT NULL, `visitCount` INTEGER NOT NULL, PRIMARY KEY(`id`))")

                insertRemoteDictProviders_5(RemoteDictionaryProviderInfo.PREDEFINED_PROVIDERS)
            }
        }
    }

    // Does effectively nothing as there's no longer search-prepared records.
    // Version 7 doesn't rely on search-prepared records and version 8 removes search-prepared records completely.
    object Migration_5_6 : Migration(5, 6) {
        override fun migrate(database: SupportSQLiteDatabase) {
        }
    }

    object Migration_10_11 : Migration(10, 11) {
        override fun migrate(database: SupportSQLiteDatabase) {
            // L% to replace new lines only in list meanings.
            database.execSQL("UPDATE records SET meaning=replace(meaning, '${ComplexMeaning.LIST_OLD_ELEMENT_SEPARATOR}', '${ComplexMeaning.LIST_NEW_ELEMENT_SEPARATOR}') WHERE meaning LIKE 'L%'")
        }
    }

    abstract fun recordDao(): RecordDao
    abstract fun remoteDictionaryProviderDao(): RemoteDictionaryProviderDao
    abstract fun remoteDictionaryProviderStatsDao(): RemoteDictionaryProviderStatsDao
    abstract fun recordBadgeDao(): RecordBadgeDao
    abstract fun recordToBadgeRelationDao(): RecordToBadgeRelationDao
    abstract fun eventDao(): EventDao
    abstract fun wordQueueDao(): WordQueueDao

    companion object {
        private var singleton: AppDatabase? = null

        // Inserts given providers to the DB with version 4
        internal fun SupportSQLiteDatabase.insertRemoteDictProviders_4(providers: Array<out RemoteDictionaryProviderInfo>) {
            val statement = compileStatement("INSERT INTO remote_dict_providers (name, schema) VALUES(?, ?)")

            runInTransactionBlocking {
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

            runInTransactionBlocking {
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
            this.addMigrations(Migration_2_3, Migration_3_4, Migration_4_5, Migration_5_6, Migration_10_11)
                .addCallback(object : Callback() {
                    override fun onCreate(db: SupportSQLiteDatabase) {
                        db.insertRemoteDictProviders_5(RemoteDictionaryProviderInfo.PREDEFINED_PROVIDERS)
                    }
                })
                .build()
    }
}