package io.github.pelmenstar1.digiDict

import android.content.Context
import android.database.Cursor
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.pelmenstar1.digiDict.backup.BackupFormat
import io.github.pelmenstar1.digiDict.backup.BackupManager
import io.github.pelmenstar1.digiDict.backup.exporting.ExportOptions
import io.github.pelmenstar1.digiDict.backup.importing.ImportOptions
import io.github.pelmenstar1.digiDict.data.Record
import io.github.pelmenstar1.digiDict.data.RecordDao
import io.github.pelmenstar1.digiDict.data.RecordTable
import io.github.pelmenstar1.digiDict.utils.AppDatabaseUtils
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.test.assertEquals

@RunWith(AndroidJUnit4::class)
class BackupManagerTests {
    private val context = ApplicationProvider.getApplicationContext<Context>()

    private fun roundtripTestHelper(format: BackupFormat, size: Int) = runTest {
        val db = AppDatabaseUtils.createTestDatabase(context)
        try {
            val file = context.getFileStreamPath("test.${format.extension}")
            file.delete()
            file.createNewFile()

            val recordDao = db.recordDao()

            val noIdRecords = Array(size) { i ->
                Record(
                    id = 0,
                    expression = "Expression$i",
                    meaning = "CMeaning$i",
                    additionalNotes = "Notes$i",
                    score = i,
                    epochSeconds = i * 1000L
                )
            }

            recordDao.insertAll(noIdRecords)
            val records = getAllRecordsNoIdInSet(recordDao)

            val exportData = BackupManager.createBackupData(db)
            file.outputStream().use {
                BackupManager.export(it, exportData, format, ExportOptions())
            }

            db.clearAllTables()

            val importData = file.inputStream().use {
                BackupManager.import(it, format, ImportOptions())
            }

            BackupManager.deployImportData(importData, db)
            val importedRecordsInDb = getAllRecordsNoIdInSet(recordDao)

            assertEquals(records, importedRecordsInDb)
        } finally {
            db.close()
        }
    }

    @Test
    fun roundtripTestDddb_1() = roundtripTestHelper(BackupFormat.DDDB, 1)

    @Test
    fun roundtripTestDddb_4() = roundtripTestHelper(BackupFormat.DDDB, 4)

    @Test
    fun roundtripTestDddb_16() = roundtripTestHelper(BackupFormat.DDDB, 16)

    @Test
    fun roundtripTestDddb_128() = roundtripTestHelper(BackupFormat.DDDB, 128)

    @Test
    fun roundtripTestDddb_1024() = roundtripTestHelper(BackupFormat.DDDB, 1024)

    @Test
    fun roundtripTestJson_1() = roundtripTestHelper(BackupFormat.JSON, 1)

    @Test
    fun roundtripTestJson_4() = roundtripTestHelper(BackupFormat.JSON, 4)

    @Test
    fun roundtripTestJson_16() = roundtripTestHelper(BackupFormat.JSON, 16)

    @Test
    fun roundtripTestJson_128() = roundtripTestHelper(BackupFormat.JSON, 128)

    @Test
    fun roundtripTestJson_1024() = roundtripTestHelper(BackupFormat.JSON, 1024)

    companion object {
        internal fun getAllRecordsNoIdInSet(dao: RecordDao): Set<RecordNoId> {
            return dao.getAllRecordsNoIdRaw().use { cursor ->
                val exprIndex = cursor.getColumnIndex { expression }
                val meaningIndex = cursor.getColumnIndex { meaning }
                val notesIndex = cursor.getColumnIndex { additionalNotes }
                val scoreIndex = cursor.getColumnIndex { score }
                val epochSecondsIndex = cursor.getColumnIndex { epochSeconds }

                val count = cursor.count
                val set = HashSet<RecordNoId>(count)

                while (cursor.moveToNext()) {
                    val expr = cursor.getString(exprIndex)
                    val meaning = cursor.getString(meaningIndex)
                    val notes = cursor.getString(notesIndex)
                    val score = cursor.getInt(scoreIndex)
                    val epochSeconds = cursor.getLong(epochSecondsIndex)

                    set.add(RecordNoId(expr, meaning, notes, score, epochSeconds))
                }

                set
            }
        }

        private inline fun Cursor.getColumnIndex(columnName: RecordTable.() -> String): Int {
            return getColumnIndex(RecordTable.columnName())
        }
    }
}