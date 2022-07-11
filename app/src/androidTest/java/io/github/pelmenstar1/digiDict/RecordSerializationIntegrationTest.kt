package io.github.pelmenstar1.digiDict

import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import io.github.pelmenstar1.digiDict.data.AppDatabase
import io.github.pelmenstar1.digiDict.data.Record
import io.github.pelmenstar1.digiDict.serialization.readValues
import io.github.pelmenstar1.digiDict.serialization.writeValues
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import kotlin.test.assertContentEquals

@RunWith(AndroidJUnit4::class)
class RecordSerializationIntegrationTest {
    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    private fun testInternal(originValues: Array<Record>) {
        val filesDir = context.filesDir
        filesDir.mkdir()

        val file = File(filesDir, "test.dvdb")
        file.delete()
        file.createNewFile()

        val appDb = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).build()
        appDb.clearAllTables()

        val dao = appDb.recordDao()
        dao.insertAll(originValues.asSequence())

        val originValuesFromDb = dao.getAllRecords()
        val allRecordsIterable = dao.getAllRecordsIterable()

        try {
            FileOutputStream(file).use {
                it.channel.writeValues(allRecordsIterable)
            }
        } finally {
            allRecordsIterable.recycle()
        }

        val valuesFromFile: Array<Record>

        FileInputStream(file).use {
            valuesFromFile = it.channel.readValues(Record.SERIALIZER).toList().toTypedArray()
        }

        assertContentEquals(originValuesFromDb, valuesFromFile)
    }

    @Test
    fun testUnderBufferSize() {
        val values = Array(5) {
            Record(
                id = 0,
                expression = "Expression",
                rawMeaning = "Meaning",
                additionalNotes = "Notes",
                score = it,
                epochSeconds = it.toLong()
            )
        }

        testInternal(values)
    }

    @Test
    fun testOverBufferSize() {
        val values = Array(1000) {
            Record(
                id = 0,
                expression = "Expression",
                rawMeaning = "Meaning",
                additionalNotes = "Notes",
                score = it,
                epochSeconds = it.toLong()
            )
        }

        testInternal(values)
    }

    @Test
    fun testEmpty() {
        testInternal(emptyArray())
    }
}