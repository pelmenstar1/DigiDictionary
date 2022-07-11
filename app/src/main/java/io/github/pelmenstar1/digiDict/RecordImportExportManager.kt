package io.github.pelmenstar1.digiDict

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.activity.result.ActivityResultCallback
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import io.github.pelmenstar1.digiDict.data.Record
import io.github.pelmenstar1.digiDict.data.RecordDao
import io.github.pelmenstar1.digiDict.serialization.readValues
import io.github.pelmenstar1.digiDict.serialization.writeValues
import io.github.pelmenstar1.digiDict.time.PackedDate
import io.github.pelmenstar1.digiDict.utils.appendPaddedFourDigit
import io.github.pelmenstar1.digiDict.utils.appendPaddedTwoDigit
import io.github.pelmenstar1.digiDict.widgets.AppWidgetUpdater
import io.github.pelmenstar1.digiDict.widgets.ListAppWidget
import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.io.FileDescriptor
import java.io.FileInputStream
import java.io.FileOutputStream
import kotlin.coroutines.resume

object RecordImportExportManager {
    private const val EXTENSION = "dvdb"
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

    // registerForActivityResult can be called only in during creation of fragment, so we register contracts during creation,
    // unregister contracts during destruction.
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

    suspend fun export(context: Context, dao: RecordDao) {
        val uri = launchAndGetResult(createDocumentLauncher, createFileName())

        uri?.useAsFile(context, mode = "w") { descriptor ->
            val allRecords = dao.getAllRecordsIterable()

            try {
                FileOutputStream(descriptor).use {
                    it.channel.writeValues(allRecords)
                }
            } finally {
                allRecords.recycle()
            }
        }
    }

    suspend fun import(context: Context, dao: RecordDao) {
        val uri = launchAndGetResult(openDocumentLauncher, mimeTypeArray)

        uri?.useAsFile(context, mode = "r") { descriptor ->
            FileInputStream(descriptor).use {
                val allRecords = it.channel.readValues(Record.SERIALIZER)

                dao.insertAll(allRecords)
            }

            withContext(Dispatchers.Main) {
                ListAppWidget.updater(context).updateAllWidgets()
            }
        }
    }

    private suspend inline fun Uri.useAsFile(
        context: Context,
        mode: String,
        crossinline block: suspend (FileDescriptor) -> Unit
    ) {
        val contentResolver = context.contentResolver
        val parcelDescriptor = requireNotNull(contentResolver.openFileDescriptor(this, mode))

        parcelDescriptor.use {
            val descriptor = it.fileDescriptor

            withContext(Dispatchers.IO) {
                block(descriptor)
            }
        }
    }

    private fun createFileName(): String {
        val nowDate = PackedDate.today()

        return buildString(32) {
            append("digi_vocabulary_")
            appendPaddedTwoDigit(nowDate.dayOfMonth)
            append('_')
            appendPaddedTwoDigit(nowDate.month)
            append('_')
            appendPaddedFourDigit(nowDate.year)

            append(".$EXTENSION")
        }
    }
}