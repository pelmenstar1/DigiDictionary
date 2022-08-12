package io.github.pelmenstar1.digiDict

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.pelmenstar1.digiDict.common.serialization.readValuesToArray
import io.github.pelmenstar1.digiDict.common.serialization.writeValues
import io.github.pelmenstar1.digiDict.data.Record
import io.github.pelmenstar1.digiDict.utils.AppDatabaseUtils
import io.github.pelmenstar1.digiDict.utils.assertContentEqualsNoId
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

@RunWith(AndroidJUnit4::class)
class RecordSerializationIntegrationTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()

    private fun testHelper(size: Int) = runTest {
        val originValues = Array(size) {
            Record(
                id = 0,
                expression = "Expression$it",
                rawMeaning = "Meaning",
                additionalNotes = "Notes",
                score = it,
                epochSeconds = it.toLong()
            )
        }

        val filesDir = context.filesDir
        filesDir.mkdir()

        val file = File(filesDir, "test.dddb")
        file.delete()
        file.createNewFile()

        val appDb = AppDatabaseUtils.createTestDatabase(context)
        try {
            val dao = appDb.recordDao()

            dao.insertAll(originValues)

            val allRecordsIterable = dao.getAllRecordsNoIdIterable()

            try {
                file.outputStream().use {
                    it.channel.writeValues(allRecordsIterable)
                }
            } finally {
                allRecordsIterable.recycle()
            }

            val valuesFromFile = file.inputStream().use {
                it.channel.readValuesToArray(Record.NO_ID_SERIALIZER_RESOLVER)
            }

            assertContentEqualsNoId(originValues, valuesFromFile)
        } finally {
            appDb.close()
        }
    }

    @Test
    fun test_1() = testHelper(1)

    @Test
    fun test_0() = testHelper(0)

    @Test
    fun test_16() = testHelper(16)

    @Test
    fun test_1024() = testHelper(1024)
}