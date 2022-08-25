package io.github.pelmenstar1.digiDict.data

import android.content.Context
import android.database.CharArrayBuffer
import androidx.room.*
import androidx.room.migration.AutoMigrationSpec
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import io.github.pelmenstar1.digiDict.common.asCharSequence
import io.github.pelmenstar1.digiDict.common.getLazyValue
import io.github.pelmenstar1.digiDict.common.runInTransitionBlocking

@Database(
    entities = [
        Record::class,
        RemoteDictionaryProviderInfo::class,
        RemoteDictionaryProviderStats::class,
        SearchPreparedRecord::class,
        RecordBadgeInfo::class,
        RecordToBadgeRelation::class
    ],
    exportSchema = true,
    version = 7,
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
        )
    ]
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    @DeleteColumn(tableName = "records", columnName = "origin")
    class Migration_1_2 : AutoMigrationSpec

    class Migration_6_7 : AutoMigrationSpec

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

    object Migration_5_6 : Migration(5, 6) {
        override fun migrate(database: SupportSQLiteDatabase) {
            database.runInTransitionBlocking {
                execSQL("CREATE TABLE search_prepared_records (id INTEGER PRIMARY KEY NOT NULL, keywords TEXT NOT NULL)")
                val insertStatement = compileStatement("INSERT INTO search_prepared_records VALUES(?, ?)")

                database.query("SELECT id, lower(expression), lower(meaning) FROM records").use { cursor ->
                    val size = cursor.count

                    val exprBuffer = CharArrayBuffer(64)

                    // In general, meaning is longer than expression.
                    val meaningBuffer = CharArrayBuffer(128)

                    val exprBufferAsCs = exprBuffer.asCharSequence()
                    val meaningBufferAsCs = meaningBuffer.asCharSequence()

                    for (i in 0 until size) {
                        cursor.moveToPosition(i)

                        val id = cursor.getLong(0)
                        cursor.copyStringToBuffer(1, exprBuffer)
                        cursor.copyStringToBuffer(2, meaningBuffer)

                        // Expression and meaning is already lowered in the query.
                        val keywords = SearchPreparedRecord.prepareToKeywords(
                            exprBufferAsCs,
                            meaningBufferAsCs,
                            needToLower = false
                        )

                        insertStatement.run {
                            // Binding index is 1-based
                            bindLong(1, id)
                            bindString(2, keywords)

                            executeInsert()
                        }
                    }
                }
            }
        }
    }

    abstract fun recordDao(): RecordDao
    abstract fun remoteDictionaryProviderDao(): RemoteDictionaryProviderDao
    abstract fun remoteDictionaryProviderStatsDao(): RemoteDictionaryProviderStatsDao
    abstract fun searchPreparedRecordDao(): SearchPreparedRecordDao
    abstract fun recordBadgeDao(): RecordBadgeDao
    abstract fun recordToBadgeRelationDao(): RecordToBadgeRelationDao

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
            this.addMigrations(Migration_2_3, Migration_3_4, Migration_4_5, Migration_5_6)
                .addCallback(object : Callback() {
                    override fun onCreate(db: SupportSQLiteDatabase) {
                        db.insertRemoteDictProviders_5(RemoteDictionaryProviderInfo.PREDEFINED_PROVIDERS)
                    }
                })
                .build()
    }
}