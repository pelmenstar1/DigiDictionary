package io.github.pelmenstar1.digiDict

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import io.github.pelmenstar1.digiDict.common.serialization.readValuesToArray
import io.github.pelmenstar1.digiDict.common.serialization.writeValues
import io.github.pelmenstar1.digiDict.data.Record
import io.github.pelmenstar1.digiDict.utils.AppDatabaseUtils
import io.github.pelmenstar1.digiDict.utils.assertContentEqualsNoId
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream

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
            val dao = appDb.recordDao()

            dao.insertAllReplace(originValues)

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

            assertContentEqualsNoId(originValues, valuesFromFile)
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