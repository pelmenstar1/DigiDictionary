package io.github.pelmenstar1.digiDict

import android.content.Context
import android.database.Cursor
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.pelmenstar1.digiDict.backup.RecordImportExportManager
import io.github.pelmenstar1.digiDict.common.serialization.readValuesToArray
import io.github.pelmenstar1.digiDict.data.Record
import io.github.pelmenstar1.digiDict.data.RecordDao
import io.github.pelmenstar1.digiDict.data.RecordTable
import io.github.pelmenstar1.digiDict.utils.AppDatabaseUtils
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.test.assertEquals

@RunWith(AndroidJUnit4::class)
class RecordImportExportManagerTests {
    private val context = ApplicationProvider.getApplicationContext<Context>()

    private fun roundtripTestHelper(size: Int) = runTest {
        val db = AppDatabaseUtils.createTestDatabase(context)
        try {
            val file = context.getFileStreamPath("test.dddb")
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

            file.outputStream().use {
                RecordImportExportManager.export(it, recordDao, null)
            }

            db.clearAllTables()

            val importedRecords = file.inputStream().use {
                it.channel.readValuesToArray(Record.NO_ID_SERIALIZER_RESOLVER)
            }

            RecordImportExportManager.import(importedRecords, db)
            val importedRecordsInDb = getAllRecordsNoIdInSet(recordDao)

            assertEquals(records, importedRecordsInDb)
        } finally {
            db.close()
        }
    }

    @Test
    fun roundtripTest_1() = roundtripTestHelper(1)

    @Test
    fun roundtripTest_4() = roundtripTestHelper(4)

    @Test
    fun roundtripTest_16() = roundtripTestHelper(16)

    @Test
    fun roundtripTest_128() = roundtripTestHelper(128)

    @Test
    fun roundtripTest_1024() = roundtripTestHelper(1024)

    @Test
    fun roundtripTest_4096() = roundtripTestHelper(4096)

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