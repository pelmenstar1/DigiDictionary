package io.github.pelmenstar1.digiDict.db

import android.database.Cursor
import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import io.github.pelmenstar1.digiDict.common.equalsPattern
import io.github.pelmenstar1.digiDict.data.*
import io.github.pelmenstar1.digiDict.utils.assertContentEqualsNoId
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.util.*
import kotlin.test.assertContentEquals
import kotlin.test.assertFails

@RunWith(AndroidJUnit4::class)
class AppDatabaseMigrationTests {
    // Matches structure of RemoteDictionaryProvider when latest DB version was 4
    data class RemoteDictionaryProvider_4(
        override val id: Int = 0, val name: String, val schema: String
    ) : EntityWithPrimaryKeyId {
        override fun equalsNoId(other: Any?) = equalsPattern(other) { o ->
            return name == o.name && schema == o.schema
        }
    }

    @get:Rule
    val helper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        AppDatabase::class.java,
        emptyList()
    )

    // Migration 2 -> 3 should add the index to records.expression in order to make it unique.
    @Test
    fun migration_2_3() {
        var db = helper.createDatabase(TEST_DB_NAME, 2)
        val expectedRecords: Array<Record>

        db.use {
            it.insertRecord("Expression1", "CMeaning1", "Notes1", score = 1)
            it.insertRecord("Expression2", "CMeaning2", "Notes2", score = 2)
            it.insertRecord("Expression3", "CMeaning3", "Notes3", score = 3)
            it.insertRecord("Expression4", "CMeaning4", "Notes4", score = 4)

            expectedRecords = it.getAllRecords()
        }

        db = helper.runMigrationsAndValidate(TEST_DB_NAME, 3, false, AppDatabase.Migration_2_3)
        val actualRecords = db.getAllRecords()

        assertContentEquals(expectedRecords, actualRecords)

        // Haven't found a better way to check if the index was added than trying to add record with duplicate expression.
        // If the index is actually added, it should throw.

        assertFails {
            db.insertRecord("Expression1", "CMeaning1", "Notes1", score = 5)
        }
    }

    // Migration 3 -> 4 should add support of remote dict. providers.
    @Test
    fun migration_3_4() {
        var db = helper.createDatabase(TEST_DB_NAME, 3)

        db.use {
            it.insertRecord("Expr1", "CMeaning1", "Notes1", score = 1)
        }

        db = helper.runMigrationsAndValidate(TEST_DB_NAME, 4, false, AppDatabase.Migration_3_4)

        val actualProviders = db.getAllRdp_4()
        assertContentEqualsNoId(PREDEFINED_PROVIDERS_4, actualProviders)

        // Insert RDP stats to check whether the table exists.
        db.insertRdpStats(0, 1)
    }

    // Migration 4 -> 5 should add urlEncodingRules column to remote_dict_providers. The migration is destructive, it
    // deletes all previous RDP's
    @Test
    fun migration_4_5() {
        var db = helper.createDatabase(TEST_DB_NAME, 4)

        db.use {
            it.insertRecord("Expr1", "CMeaning1", "Notes1", score = 1)
        }

        db = helper.runMigrationsAndValidate(TEST_DB_NAME, 5, false, AppDatabase.Migration_4_5)

        val actualProviders = db.getAllRdp_5()
        assertContentEqualsNoId(RemoteDictionaryProviderInfo.PREDEFINED_PROVIDERS, actualProviders)

        // Insert RDP stats to check whether the table exists.
        db.insertRdpStats(0, 1)
    }

    // Migration 5 -> 6 should add and fill search_prepared_records table.
    @Test
    fun migration_5_6() {
        val expr1 = "Expression test, some other stuff"
        val expr2 = "Expression2, test; s; 1 2"

        val meaning1 = "CMeaning and others: 1, 2, 3"
        val meaning2 = "L3@M A A \n Bacccc bbb \n 12 ffb; afb"

        var db = helper.createDatabase(TEST_DB_NAME, 5)

        db.use {
            it.insertRecord(expr1, meaning1, "Notes", 1)
            it.insertRecord(expr2, meaning2, "Notes", 1)
        }

        val expectedSearchRecords = arrayOf(
            SearchPreparedRecord.prepare(0, expr1, meaning1, Locale.ROOT),
            SearchPreparedRecord.prepare(0, expr2, meaning2, Locale.ROOT)
        )

        db = helper.runMigrationsAndValidate(TEST_DB_NAME, 6, false, AppDatabase.Migration_5_6)

        val actualSearchRecords = db.getAllSearchRecords()

        assertContentEqualsNoId(expectedSearchRecords, actualSearchRecords)
    }

    companion object {
        private const val TEST_DB_NAME = "database-test"

        private val PREDEFINED_PROVIDERS_4 = arrayOf(
            RemoteDictionaryProvider_4(
                name = "Cambridge English Dictionary",
                schema = "https://dictionary.cambridge.org/dictionary/english/\$query$",
            ),
            RemoteDictionaryProvider_4(
                name = "Urban Dictionary",
                schema = "https://www.urbandictionary.com/define.php?term=\$query$",
            )
        )

        // DAO's can't be used because they expect the newest schema.
        // That's why everything is done manually without beloved code generation.

        private fun SupportSQLiteDatabase.insertRecord(expr: String, rawMeaning: String, notes: String, score: Int) {
            val epochSeconds = System.currentTimeMillis()

            execSQL(
                "INSERT INTO records (`expression`, `meaning`, `additionalNotes`, `score`, `dateTime`) VALUES (?, ?, ?, ?, ?)",
                arrayOf(expr, rawMeaning, notes, score, epochSeconds)
            )
        }

        private fun SupportSQLiteDatabase.getAllRecords(): Array<Record> {
            return querySelectAll(tableName = "records") {
                val id = it.getInt(0)
                val expression = it.getString(1)
                val rawMeaning = it.getString(2)
                val notes = it.getString(3)
                val score = it.getInt(4)
                val epochSeconds = it.getLong(5)

                Record(id, expression, rawMeaning, notes, score, epochSeconds)
            }
        }

        private fun SupportSQLiteDatabase.getAllRdp_4(): Array<RemoteDictionaryProvider_4> {
            return querySelectAll(tableName = "remote_dict_providers") {
                val id = it.getInt(0)
                val name = it.getString(1)
                val schema = it.getString(2)

                RemoteDictionaryProvider_4(id, name, schema)
            }
        }

        private fun SupportSQLiteDatabase.getAllRdp_5(): Array<RemoteDictionaryProviderInfo> {
            return querySelectAll(tableName = "remote_dict_providers") {
                val id = it.getInt(0)
                val name = it.getString(1)
                val schema = it.getString(2)
                val urlEncodingRules = it.getString(3)

                RemoteDictionaryProviderInfo(
                    id,
                    name,
                    schema,
                    RemoteDictionaryProviderInfo.UrlEncodingRules(urlEncodingRules)
                )
            }
        }

        private fun SupportSQLiteDatabase.insertRdpStats(id: Int, visitCount: Int) {
            execSQL("INSERT INTO remote_dict_provider_stats VALUES(?, ?)", arrayOf(id, visitCount))
        }

        private fun SupportSQLiteDatabase.getAllSearchRecords(): Array<SearchPreparedRecord> {
            return querySelectAll(tableName = "search_prepared_records") {
                val id = it.getInt(0)
                val keywords = it.getString(1)

                SearchPreparedRecord(id, keywords)
            }
        }

        private inline fun <reified T> SupportSQLiteDatabase.querySelectAll(
            tableName: String,
            convert: (Cursor) -> T
        ): Array<T> {
            return query("SELECT * FROM $tableName").use { cursor ->
                val size = cursor.count

                Array(size) { i ->
                    cursor.moveToPosition(i)

                    convert(cursor)
                }
            }
        }
    }
}