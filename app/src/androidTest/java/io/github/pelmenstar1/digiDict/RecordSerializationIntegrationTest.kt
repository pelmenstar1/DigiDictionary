package io.github.pelmenstar1.digiDict

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import io.github.pelmenstar1.digiDict.data.Record
import io.github.pelmenstar1.digiDict.serialization.readValuesToArray
import io.github.pelmenstar1.digiDict.serialization.writeValues
import io.github.pelmenstar1.digiDict.utils.AppDatabaseUtils
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import kotlin.test.assertTrue

@RunWith(AndroidJUnit4::class)
class RecordSerializationIntegrationTest {
    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    private fun testInternal(originValues: Array<Record>) {
        runBlocking {
            val filesDir = context.filesDir
            filesDir.mkdir()

            val file = File(filesDir, "test.dddb")
            file.delete()
            file.createNewFile()

            val appDb = AppDatabaseUtils.createTestDatabase(context)
            appDb.clearAllTables()

            val dao = appDb.recordDao()

            dao.insertAll(originValues.asList())

            val originValuesFromDb = dao.getAllRecords()
            val allRecordsIterable = dao.getAllRecordsNoIdIterable()

            try {
                FileOutputStream(file).use {
                    it.channel.writeValues(allRecordsIterable)
                }
            } finally {
                allRecordsIterable.recycle()
            }

            val valuesFromFile: Array<Record>

            FileInputStream(file).use {
                valuesFromFile = it.channel.readValuesToArray(Record.NO_ID_SERIALIZER)
            }

            val isOriginEqualsToFileValues = run {
                val size = valuesFromFile.size
                if (originValuesFromDb.size != size) return@run false

                for (i in 0 until size) {
                    if (!originValuesFromDb[i].equalsNoId(valuesFromFile[i])) {
                        return@run false
                    }
                }

                true
            }

            assertTrue(isOriginEqualsToFileValues)
        }
    }

    @Test
    fun testUnderBufferSize() {
        val values = Array(5) {
            Record(
                id = 0,
                expression = "Expression$it",
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
                expression = "Expression$it",
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