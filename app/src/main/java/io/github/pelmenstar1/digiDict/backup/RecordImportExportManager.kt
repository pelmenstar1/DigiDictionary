package io.github.pelmenstar1.digiDict.backup

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.activity.result.ActivityResultCallback
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import io.github.pelmenstar1.digiDict.RecordExpressionDuplicateException
import io.github.pelmenstar1.digiDict.common.appendPaddedFourDigit
import io.github.pelmenstar1.digiDict.common.appendPaddedTwoDigit
import io.github.pelmenstar1.digiDict.common.getLocaleCompat
import io.github.pelmenstar1.digiDict.common.serialization.readValuesToArray
import io.github.pelmenstar1.digiDict.common.serialization.writeValues
import io.github.pelmenstar1.digiDict.data.Record
import io.github.pelmenstar1.digiDict.data.RecordDao
import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.io.FileDescriptor
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.*
import kotlin.coroutines.resume

object RecordImportExportManager {
    private const val EXTENSION = "dddb"
    private const val MIME_TYPE = "*/*"

    private val mimeTypeArray = arrayOf(MIME_TYPE)

    private val pendingContLock = Any()
    private var pendingContinuation: CancellableContinuation<Uri?>? = null

    private val createDocumentContract =
        object : ActivityResultContracts.CreateDocument(MIME_TYPE) {
            override fun createIntent(context: Context, input: String): Intent {
                return super.createIntent(context, input).also {
                    it.type = MIME_TYPE
                    it.addCategory(Intent.CATEGORY_OPENABLE)
                }
            }
        }

    private val openDocumentContract = object : ActivityResultContracts.OpenDocument() {
        override fun createIntent(context: Context, input: Array<String>): Intent {
            return super.createIntent(context, input).also {
                //it.type = MIME_TYPE
                it.addCategory(Intent.CATEGORY_OPENABLE)
            }
        }
    }

    private var createDocumentLauncher: ActivityResultLauncher<String>? = null
    private var openDocumentLauncher: ActivityResultLauncher<Array<String>>? = null

    // registerForActivityResult can be called only during the creation of fragment,
    // so we register contracts during creation, unregister contracts during destruction.
    fun init(fragment: Fragment) {
        val callback = ActivityResultCallback<Uri?> {
            synchronized(pendingContLock) { pendingContinuation?.resume(it) }
        }

        with(fragment) {
            createDocumentLauncher = registerForActivityResult(createDocumentContract, callback)
            openDocumentLauncher = registerForActivityResult(openDocumentContract, callback)
        }
    }

    fun release() {
        synchronized(pendingContLock) {
            pendingContinuation?.cancel()
            pendingContinuation = null
        }

        createDocumentLauncher?.let {
            it.unregister()
            createDocumentLauncher = null
        }

        openDocumentLauncher?.let {
            it.unregister()
            openDocumentLauncher = null
        }
    }

    private suspend fun <I> launchAndGetResult(
        launcher: ActivityResultLauncher<I>?,
        input: I
    ): Uri? {
        return withContext(Dispatchers.Main) {
            suspendCancellableCoroutine { cont ->
                synchronized(pendingContLock) { pendingContinuation = cont }

                launcher?.launch(input)
            }
        }
    }

    /**
     * Returns true if there was an attempt to export data. If an user doesn't choose any file, returns false.
     */
    suspend fun export(context: Context, dao: RecordDao): Boolean {
        val fileName = createFileName(context.getLocaleCompat())
        val uri = launchAndGetResult(createDocumentLauncher, fileName)

        return if (uri != null) {
            withContext(Dispatchers.IO) {
                uri.useAsFile(context, mode = "w") { descriptor ->
                    val allRecords = dao.getAllRecordsNoIdIterable()

                    try {
                        FileOutputStream(descriptor).use {
                            it.channel.writeValues(allRecords)
                        }
                    } finally {
                        allRecords.recycle()
                    }
                }

                true
            }
        } else {
            false
        }
    }

    /**
     * Returns true if the file is chosen and there is an attempt to import the data. Otherwise, returns false.
     */
    @Suppress("UNCHECKED_CAST", "ReplaceNotNullAssertionWithElvisReturn")
    suspend fun import(
        context: Context,
        recordDao: RecordDao
    ): Boolean {
        val uri = launchAndGetResult(openDocumentLauncher, mimeTypeArray)

        if (uri != null) {
            return withContext(Dispatchers.Default) {
                val importedRecords = uri.useAsFile(context, mode = "r") { descriptor ->
                    FileInputStream(descriptor).use {
                        it.channel.readValuesToArray(Record.NO_ID_SERIALIZER)
                    }
                }

                import(importedRecords, recordDao)

                true
            }
        }

        return true
    }

    suspend fun import(records: Array<Record>, recordDao: RecordDao) {
        if (records.isNotEmpty()) {
            Arrays.sort(records, Record.EXPRESSION_COMPARATOR)

            val recordsSize = records.size

            records.forEachIndexed { index, record ->
                val expr = record.expression

                // This works because importedRecords array is sorted by expression,
                // which means that two equal expressions will be subsequent and if it's, throw an exception.
                val nextIndex = index + 1
                if (nextIndex < recordsSize) {
                    if (expr == records[nextIndex].expression) {
                        throw RecordExpressionDuplicateException()
                    }
                }
            }

            recordDao.insertAllReplace(records)
        }
    }

    private inline fun <R> Uri.useAsFile(
        context: Context,
        mode: String,
        block: (FileDescriptor) -> R
    ): R {
        val contentResolver = context.contentResolver
        val parcelDescriptor = requireNotNull(contentResolver.openFileDescriptor(this, mode))

        return parcelDescriptor.use {
            block(it.fileDescriptor)
        }
    }

    private fun createFileName(locale: Locale): String {
        val calendar = Calendar.getInstance(locale)

        return buildString(32) {
            append("digi_dict_")
            appendPaddedTwoDigit(calendar[Calendar.DAY_OF_MONTH])
            append('_')

            // Month is 0-based in Calendar.
            appendPaddedTwoDigit(calendar[Calendar.MONTH] + 1)
            append('_')
            appendPaddedFourDigit(calendar[Calendar.YEAR])

            append(".$EXTENSION")
        }
    }
}