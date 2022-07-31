package io.github.pelmenstar1.digiDict

import android.content.ContentValues
import android.net.Uri
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import io.github.pelmenstar1.digiDict.data.ComplexMeaning
import io.github.pelmenstar1.digiDict.data.Record
import io.github.pelmenstar1.digiDict.serialization.writeValues
import io.github.pelmenstar1.digiDict.time.SECONDS_IN_HOUR
import org.junit.Ignore
import org.junit.Test
import org.junit.runner.RunWith
import java.io.FileOutputStream
import java.io.IOException
import kotlin.random.Random

// Generates DDDB file which contains records whose expression has same format as in DebugDatabaseTest (Expression1, Expression2)
// but meaning has _dup suffix to allow to test "Merge" in ResolveConflictsFragment.
@RunWith(AndroidJUnit4::class)
@Ignore("Should be run manually") // remove this to run the test.
class DuplicateRecordFileGenerator {
    private val appContext = InstrumentationRegistry.getInstrumentation().targetContext

    @Test
    fun generate() {
        val nowEpochSeconds = System.currentTimeMillis() / 1000
        var epochSeconds = nowEpochSeconds

        val records = Array(size = 10) { i ->
            val ordinal = i + 1

            Record(
                id = 0,
                expression = "Expression$ordinal",
                rawMeaning = ComplexMeaning.Common("Meaning${ordinal}_dup").rawText,
                additionalNotes = "Notes$ordinal",
                score = 0,
                epochSeconds = epochSeconds
            ).also {
                epochSeconds += SECONDS_IN_HOUR
            }
        }

        val values = ContentValues().apply {
            val fileName = "generated_digiDictDb_${Random.Default.nextInt(from = 0, until = Int.MAX_VALUE)}.dddb"

            put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
            put(MediaStore.MediaColumns.MIME_TYPE, "application/dddb")
            put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
        }

        val resolver = appContext.contentResolver
        var uri: Uri? = null

        try {
            uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)

            if(uri == null) {
                Log.e(TAG, "Failed to create file")
                throw IOException()
            }

            val descriptor = resolver.openFileDescriptor(uri, "w") ?: throw IOException()

            descriptor.use {
                FileOutputStream(descriptor.fileDescriptor).use {
                    it.channel.writeValues(records, Record.NO_ID_SERIALIZER)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, null, e)

            if(uri != null) {
                resolver.delete(uri, null, null)
            }
        }
    }

    companion object {
        private const val TAG = "DupRecordFileGenerator"
    }
}