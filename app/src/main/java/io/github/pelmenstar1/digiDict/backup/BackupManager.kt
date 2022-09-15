package io.github.pelmenstar1.digiDict.backup

import android.content.Context
import android.net.Uri
import androidx.room.withTransaction
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
import io.github.pelmenstar1.digiDict.common.fileExtensionOrNull
import io.github.pelmenstar1.digiDict.data.AppDatabase
import io.github.pelmenstar1.digiDict.data.RecordBadgeInfo
import io.github.pelmenstar1.digiDict.data.RecordToBadgeRelation
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

    suspend fun createBackupData(appDatabase: AppDatabase, options: ExportOptions): BackupData {
        val recordDao = appDatabase.recordDao()
        val badgeDao = appDatabase.recordBadgeDao()
        val recordToBadgeRelationDao = appDatabase.recordToBadgeRelationDao()

        val records = recordDao.getAllRecordsByIdAsc()
        val badges: Array<RecordBadgeInfo>
        val badgeToMultipleRecordEntries: Array<BackupBadgeToMultipleRecordEntry>

        if (options.exportBadges) {
            badges = badgeDao.getAllOrderByIdAsc()
            val recordToBadgeRelations = recordToBadgeRelationDao.getAllOrderByBadgeIdAsc()
            val recordIdToOrdinalMap = IdToOrdinalMap(records)

            badgeToMultipleRecordEntries =
                BackupHelpers.groupRecordToBadgeRelations(recordToBadgeRelations, recordIdToOrdinalMap)
        } else {
            badges = emptyArray()
            badgeToMultipleRecordEntries = emptyArray()
        }

        return BackupData(records, badges, badgeToMultipleRecordEntries)
    }

    fun export(
        output: OutputStream,
        data: BackupData,
        format: BackupFormat,
        progressReporter: ProgressReporter? = null
    ) {
        val exporter = exporters[format] ?: throw RuntimeException("No exporter assigned for given format ($format)")

        exporter.export(output, data, progressReporter)
    }

    fun export(
        context: Context,
        uri: Uri,
        data: BackupData,
        format: BackupFormat,
        progressReporter: ProgressReporter? = null
    ) {
        uri.useAsFile(context, mode = "w") {
            export(FileOutputStream(it), data, format, progressReporter)
        }
    }

    fun import(
        input: InputStream,
        format: BackupFormat,
        options: ImportOptions,
        progressReporter: ProgressReporter? = null
    ): BackupData {
        val importer = importers[format] ?: throw RuntimeException("No importer assigned for given format ($format)")
        val data = importer.import(input, options, progressReporter?.subReporter(completed = 0f, target = .9f))

        verifyImportData(data)
        progressReporter?.end()

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
        val ext = uri.fileExtensionOrNull() ?: throw RuntimeException("Invalid URI")

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
        if (BackupHelpers.containsDuplicateExpressions(data.records)) {
            throw RecordExpressionDuplicateException()
        }
    }

    suspend fun deployImportData(data: BackupData, options: ImportOptions, appDatabase: AppDatabase) {
        appDatabase.withTransaction {
            val recordDao = appDatabase.recordDao()
            val badgeDao = appDatabase.recordBadgeDao()
            val recordToBadgeRelationDao = appDatabase.recordToBadgeRelationDao()

            val records = data.records
            val badges = data.badges
            val badgeToMultipleRecordEntries = data.badgeToMultipleRecordEntries

            if (options.importBadges && badges.isNotEmpty()) {
                val replaceBadges = options.replaceBadges

                val sortedRecordIds = IntArray(records.size)
                val sortedBadgeIds = IntArray(badges.size)

                for (i in records.indices) {
                    val record = records[i]
                    val actualId = recordDao.insertReplace(record)

                    sortedRecordIds[i] = actualId.toInt()
                }

                for (i in badges.indices) {
                    val badge = badges[i]

                    sortedBadgeIds[i] = if (replaceBadges) {
                        badgeDao.insertReplace(badge).toInt()
                    } else {
                        badgeDao.getIdByName(badge.name) ?: badgeDao.insert(badge).toInt()
                    }
                }

                Arrays.sort(sortedBadgeIds)
                Arrays.sort(sortedRecordIds)

                for ((badgeOrdinal, recordOrdinals) in badgeToMultipleRecordEntries) {
                    recordOrdinals.forEach { recordOrdinal ->
                        val recordId = sortedRecordIds[recordOrdinal]
                        val badgeId = sortedBadgeIds[badgeOrdinal]
                        val relation = RecordToBadgeRelation(recordId = recordId, badgeId = badgeId)

                        recordToBadgeRelationDao.insertIgnore(relation)
                    }
                }
            } else {
                recordDao.insertAllReplace(data.records)
            }
        }
    }
}