package io.github.pelmenstar1.digiDict.backup

import android.content.Context
import android.net.Uri
import io.github.pelmenstar1.digiDict.RecordExpressionDuplicateException
import io.github.pelmenstar1.digiDict.backup.exporting.BinaryDataExporter
import io.github.pelmenstar1.digiDict.backup.exporting.DataExporter
import io.github.pelmenstar1.digiDict.backup.exporting.ExportOptions
import io.github.pelmenstar1.digiDict.backup.exporting.JsonDataExporter
import io.github.pelmenstar1.digiDict.backup.importing.BinaryDataImporter
import io.github.pelmenstar1.digiDict.backup.importing.DataImporter
import io.github.pelmenstar1.digiDict.backup.importing.ImportOptions
import io.github.pelmenstar1.digiDict.backup.importing.JsonDataImporter
import io.github.pelmenstar1.digiDict.common.ProgressReporter
import io.github.pelmenstar1.digiDict.data.AppDatabase
import io.github.pelmenstar1.digiDict.data.Record
import java.io.*
import java.util.*

object BackupManager {
    private val exporters = EnumMap<BackupFormat, DataExporter>(BackupFormat::class.java).apply {
        put(BackupFormat.DDDB, BinaryDataExporter())
        put(BackupFormat.JSON, JsonDataExporter())
    }

    private val importers = EnumMap<BackupFormat, DataImporter>(BackupFormat::class.java).apply {
        put(BackupFormat.DDDB, BinaryDataImporter())
        put(BackupFormat.JSON, JsonDataImporter())
    }

    suspend fun createBackupData(appDatabase: AppDatabase): BackupData {
        val recordDao = appDatabase.recordDao()
        val records = recordDao.getAllRecords()

        return BackupData(records)
    }

    fun export(
        output: OutputStream,
        data: BackupData,
        format: BackupFormat,
        options: ExportOptions,
        progressReporter: ProgressReporter? = null
    ) {
        val exporter = exporters[format] ?: throw RuntimeException("No exporter assigned for given format ($format)")

        exporter.export(output, data, options, progressReporter)
    }

    fun export(
        context: Context,
        uri: Uri,
        data: BackupData,
        format: BackupFormat,
        options: ExportOptions,
        progressReporter: ProgressReporter? = null
    ) {
        uri.useAsFile(context, mode = "w") {
            export(FileOutputStream(it), data, format, options, progressReporter)
        }
    }

    fun import(
        input: InputStream,
        format: BackupFormat,
        options: ImportOptions,
        progressReporter: ProgressReporter? = null
    ): BackupData {
        val importer = importers[format] ?: throw RuntimeException("No importer assigned for given format ($format)")
        val data = importer.import(input, options, progressReporter)
        verifyImportData(data)

        return data
    }

    fun import(
        context: Context,
        uri: Uri,
        options: ImportOptions,
        progressReporter: ProgressReporter? = null
    ): BackupData {
        return uri.useAsFile(context, mode = "r") {
            val format = getBackupFormatForUri(uri)

            import(FileInputStream(it), format, options, progressReporter)
        }
    }

    fun getBackupFormatForUri(uri: Uri): BackupFormat {
        val path = uri.path ?: throw RuntimeException("Invalid uri")
        val lastDotIndex = path.lastIndexOf('.')
        if (lastDotIndex < 0 || lastDotIndex == path.length - 1) {
            throw RuntimeException("Invalid uri")
        }

        val ext = path.substring(lastDotIndex + 1)
        return BackupFormat.fromExtension(ext)
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

    private fun verifyImportData(data: BackupData) {
        val records = data.records
        val recordsSize = records.size

        // Verifies that import data doesn't contain any duplicate expressions.
        Arrays.sort(records, Record.EXPRESSION_COMPARATOR)
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
    }

    suspend fun deployImportData(data: BackupData, appDatabase: AppDatabase) {
        val recordDao = appDatabase.recordDao()

        recordDao.insertAll(data.records)
    }
}