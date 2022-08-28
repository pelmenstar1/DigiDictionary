package io.github.pelmenstar1.digiDict

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.pelmenstar1.digiDict.backup.RecordImportExportManager
import io.github.pelmenstar1.digiDict.common.mapToHashSet
import io.github.pelmenstar1.digiDict.common.serialization.readValuesToArray
import io.github.pelmenstar1.digiDict.data.Record
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
            val records = recordDao.getAllRecords()

            file.outputStream().use {
                RecordImportExportManager.export(it, recordDao, null)
            }

            db.clearAllTables()

            val importedRecords = file.inputStream().use {
                it.channel.readValuesToArray(Record.NO_ID_SERIALIZER_RESOLVER)
            }

            RecordImportExportManager.import(importedRecords, db)

            val importedRecordsInDb = recordDao.getAllRecords().mapToHashSet(RecordNoId::fromRecord)

            assertEquals(records.mapToHashSet(RecordNoId::fromRecord), importedRecordsInDb)
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
}