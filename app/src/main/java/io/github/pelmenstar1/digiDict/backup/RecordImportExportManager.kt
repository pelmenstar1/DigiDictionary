package io.github.pelmenstar1.digiDict.backup

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.activity.result.ActivityResultCallback
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import io.github.pelmenstar1.digiDict.RecordExpressionDuplicateException
import io.github.pelmenstar1.digiDict.data.ConflictEntry
import io.github.pelmenstar1.digiDict.data.Record
import io.github.pelmenstar1.digiDict.data.RecordDao
import io.github.pelmenstar1.digiDict.serialization.readValuesToList
import io.github.pelmenstar1.digiDict.serialization.writeValues
import io.github.pelmenstar1.digiDict.time.PackedDate
import io.github.pelmenstar1.digiDict.utils.appendPaddedFourDigit
import io.github.pelmenstar1.digiDict.utils.appendPaddedTwoDigit
import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.io.FileDescriptor
import java.io.FileInputStream
import java.io.FileOutputStream
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

        withContext(Dispatchers.IO) {
            uri?.useAsFile(context, mode = "w") { descriptor ->
                val allRecords = dao.getAllRecordsNoIdIterable()

                try {
                    FileOutputStream(descriptor).use {
                        it.channel.writeValues(allRecords)
                    }
                } finally {
                    allRecords.recycle()
                }
            }
        }
    }

    /**
     * Shows system's file chooser and handles the import of records.
     * If records are inserted to the DB in the end of execution of the method, returns `false` which means no further actions required.
     * If there are conflicts, returns `true` which means ResolveImportConflictsFragment should be shown.
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
                        it.channel.readValuesToList(Record.NO_ID_SERIALIZER)
                    }
                }

                if (importedRecords.isNotEmpty()) {
                    importedRecords.sortWith(Record.EXPRESSION_COMPARATOR)

                    val allRecordExpressions = recordDao.getAllExpressions()
                    allRecordExpressions.sort()

                    val conflicts = ArrayList<ConflictEntry>()

                    val importedRecordsToRemove = ArrayList<Record>()
                    val importedRecordsSize = importedRecords.size

                    importedRecords.forEachIndexed { index, importedRecord ->
                        val expr = importedRecord.expression

                        // This works because importedRecords list is sorted by expression,
                        // which means that two equal expressions will be subsequent and if it's, throw an exception.
                        val nextIndex = index + 1
                        if (nextIndex < importedRecordsSize) {
                            if (expr == importedRecords[nextIndex].expression) {
                                throw RecordExpressionDuplicateException()
                            }
                        }

                        // allRecordExpressions is sorted.
                        val recordIndex = allRecordExpressions.binarySearch(expr)

                        if (recordIndex >= 0) {
                            val old = recordDao.getRecordByExpression(expr)

                            // As recordIndex is positive, record with expression expr should be in the DB.
                            // This check is here to make the compiler happy and just to make sure.
                            if (old != null) {
                                // Add to conflicts only if at least one property is different.
                                if (importedRecord.score != old.score ||
                                    importedRecord.epochSeconds != old.epochSeconds ||
                                    importedRecord.rawMeaning != old.rawMeaning ||
                                    importedRecord.additionalNotes != old.additionalNotes
                                ) {
                                    val entry = ConflictEntry.fromRecordPair(id = old.id, old, new = importedRecord)
                                    conflicts.add(entry)
                                }
                            }

                            // We need to remove a record from importedRecords if 'records' contains it.
                            // See TemporaryImportStorage.
                            //
                            // As we are iterating importedRecords we cannot just remove element from it.
                            // Instead 'old' element is saved to a temporary list and will be removed later.
                            importedRecordsToRemove.add(importedRecord)

                        }
                    }

                    if (importedRecordsToRemove.isNotEmpty()) {
                        importedRecords.removeAll(importedRecordsToRemove)
                    }

                    if (conflicts.isEmpty()) {
                        recordDao.insertAll(importedRecords)

                        false
                    } else {
                        TemporaryImportStorage.also { storage ->
                            storage.importedRecords = importedRecords
                            storage.conflictEntries = conflicts
                        }

                        true
                    }
                } else {
                    false
                }
            }
        }

        return false
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

    private fun createFileName(): String {
        val nowDate = PackedDate.today()

        return buildString(32) {
            append("digi_dict_")
            appendPaddedTwoDigit(nowDate.dayOfMonth)
            append('_')
            appendPaddedTwoDigit(nowDate.month)
            append('_')
            appendPaddedFourDigit(nowDate.year)

            append(".$EXTENSION")
        }
    }
}