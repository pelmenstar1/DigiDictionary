package io.github.pelmenstar1.digiDict.backup

import android.content.Context
import android.net.Uri
import androidx.sqlite.db.SupportSQLiteStatement
import io.github.pelmenstar1.digiDict.RecordExpressionDuplicateException
import io.github.pelmenstar1.digiDict.backup.exporting.BinaryDataExporter
import io.github.pelmenstar1.digiDict.backup.exporting.DataExporter
import io.github.pelmenstar1.digiDict.backup.exporting.ExportOptions
import io.github.pelmenstar1.digiDict.backup.exporting.JsonDataExporter
import io.github.pelmenstar1.digiDict.backup.importing.*
import io.github.pelmenstar1.digiDict.common.ProgressReporter
import io.github.pelmenstar1.digiDict.common.mapToArray
import io.github.pelmenstar1.digiDict.common.trackLoopProgressWith
import io.github.pelmenstar1.digiDict.common.trackProgressWith
import io.github.pelmenstar1.digiDict.data.*
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

    val latestCompatInfo = BackupCompatInfo(newMeaningFormat = true)

    fun createBackupData(
        appDatabase: AppDatabase,
        options: ExportOptions,
        compatInfo: BackupCompatInfo = latestCompatInfo,
        progressReporter: ProgressReporter? = null
    ): BackupData {
        var records: Array<Record>
        val badges: Array<RecordBadgeInfo>
        val badgeToMultipleRecordEntries: Array<BackupBadgeToMultipleRecordEntry>

        return trackProgressWith(progressReporter) {
            if (options.exportBadges) {
                records = appDatabase.getAllRecordsOrderByIdAsc(
                    progressReporter?.subReporter(completed = 0, target = 33)
                )

                badges = appDatabase.getAllRecordBadgesOrderByIdAsc(
                    progressReporter?.subReporter(completed = 33, target = 67)
                )

                val recordToBadgeRelations = appDatabase.getAllRecordToBadgeRelations(
                    progressReporter?.subReporter(completed = 67, target = 100)
                )

                badgeToMultipleRecordEntries = BackupHelpers.groupRecordToBadgeRelations(
                    recordToBadgeRelations,
                    IdToOrdinalMap(records)
                )
            } else {
                records = appDatabase.getAllRecordsOrderByIdAsc(progressReporter)
                badges = emptyArray()
                badgeToMultipleRecordEntries = emptyArray()
            }

            // Currently, non-latest compat info is used only in the tests,
            // so the implementation doesn't have to be efficient.
            if (!compatInfo.newMeaningFormat) {
                records = records.mapToArray {
                    val recodedMeaning = ComplexMeaning.recodeListNewFormatToOld(it.meaning)

                    Record(it.id, it.expression, recodedMeaning, it.additionalNotes, it.score, it.epochSeconds)
                }
            }

            BackupData(records, badges, badgeToMultipleRecordEntries, compatInfo)
        }
    }

    fun export(
        output: OutputStream,
        data: BackupData,
        format: BackupFormat,
        version: Int,
        progressReporter: ProgressReporter? = null
    ) {
        val exporter = exporters[format] ?: throw RuntimeException("No exporter assigned for given format ($format)")

        exporter.export(output, data, version, progressReporter)
    }

    fun export(
        context: Context,
        uri: Uri,
        data: BackupData,
        format: BackupFormat,
        version: Int,
        progressReporter: ProgressReporter? = null
    ) {
        uri.useAsFile(context, mode = "w") {
            export(FileOutputStream(it), data, format, version, progressReporter)
        }
    }

    fun import(
        input: InputStream,
        format: BackupFormat,
        options: ImportOptions,
        progressReporter: ProgressReporter? = null
    ): BackupData {
        val importer = importers[format] ?: throw RuntimeException("No importer assigned for given format ($format)")

        return trackProgressWith(progressReporter) {
            // Verifying the data can take some time, so let it be 10% of work.
            importer.import(input, options, progressReporter?.subReporter(completed = 0, target = 90)).also {
                verifyImportData(it)
            }
        }
    }

    fun import(
        context: Context,
        uri: Uri,
        format: BackupFormat,
        options: ImportOptions,
        progressReporter: ProgressReporter? = null
    ): BackupData {
        return uri.useAsFile(context, mode = "r") {
            import(FileInputStream(it), format, options, progressReporter)
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

    private fun verifyImportData(data: BackupData) {
        if (BackupHelpers.containsDuplicateExpressions(data.records)) {
            throw RecordExpressionDuplicateException()
        }
    }

    fun deployImportData(
        data: BackupData,
        options: ImportOptions,
        appDatabase: AppDatabase,
        progressReporter: ProgressReporter? = null
    ) {
        appDatabase.runInTransaction {
            trackProgressWith(progressReporter) {
                val records = data.records
                val badges = data.badges
                val compatInfo = data.compatInfo

                if (options.importBadges && badges.isNotEmpty()) {
                    val replaceBadges = options.replaceBadges

                    // from 0/3 to 1/3 of the total work
                    val recordReporter = progressReporter?.subReporter(completed = 0, target = 33)

                    // 1/3 to 2/3 of the total work
                    val badgeReporter = progressReporter?.subReporter(completed = 33, target = 67)

                    // 2/3 to 3/3 of the total work
                    val recordToBadgeRelationReporter = progressReporter?.subReporter(completed = 67, target = 100)

                    val sortedRecordIds =
                        insertRecordsAndCreateIdToOrdinalMap(appDatabase, records, compatInfo, recordReporter)
                    val sortedBadgeIds = if (replaceBadges) {
                        insertOrReplaceRecordBadgesAndCreateIdToOrdinalMap(appDatabase, badges, badgeReporter)
                    } else {
                        insertPreserveRecordBadgesAndCreateIdToOrdinalMap(appDatabase, badges, badgeReporter)
                    }

                    insertRecordToBadgeRelations(
                        appDatabase,
                        data.badgeToMultipleRecordEntries,
                        sortedRecordIds,
                        sortedBadgeIds,
                        recordToBadgeRelationReporter
                    )
                } else {
                    appDatabase.compileInsertRecordStatement().use { statement ->
                        trackLoopProgressWith(progressReporter, records) { _, record ->
                            bindRecordToInsertStatementWithRecoding(statement, record, compatInfo)
                            statement.executeInsert()
                        }
                    }
                }
            }
        }
    }

    private fun bindRecordToInsertStatementWithRecoding(
        statement: SupportSQLiteStatement,
        record: Record,
        compatInfo: BackupCompatInfo
    ) {
        var meaning = record.meaning

        if (!compatInfo.newMeaningFormat) {
            // Recode only list meanings.
            if (meaning[0] == ComplexMeaning.LIST_MARKER) {
                meaning = ComplexMeaning.recodeListOldFormatToNew(meaning)
            }
        }

        if (!ComplexMeaning.isValid(meaning)) {
            throw ImportException(ImportException.REASON_DATA_VALIDATION, "Invalid meaning format")
        }

        statement.bindRecordToInsertStatement(
            record.expression,
            meaning,
            record.additionalNotes,
            record.score,
            record.epochSeconds
        )
    }

    private inline fun <T> insertEntitiesAndCreateIdToOrdinalMap(
        appDatabase: AppDatabase,
        entities: Array<out T>,
        progressReporter: ProgressReporter?,
        compileStatement: (AppDatabase) -> SupportSQLiteStatement,
        bind: (SupportSQLiteStatement, T) -> Unit
    ): IdToOrdinalMap {
        return compileStatement(appDatabase).use { statement ->
            val map = IdToOrdinalMap(entities.size)

            trackLoopProgressWith(progressReporter, entities) { _, entity ->
                bind(statement, entity)

                val id = statement.executeInsert().toInt()
                map.add(id)
            }

            map
        }
    }

    private fun insertRecordsAndCreateIdToOrdinalMap(
        appDatabase: AppDatabase,
        records: Array<out Record>,
        compatInfo: BackupCompatInfo,
        progressReporter: ProgressReporter?
    ) = insertEntitiesAndCreateIdToOrdinalMap(
        appDatabase,
        records,
        progressReporter,
        AppDatabase::compileInsertRecordStatement,
        bind = { statement, record -> bindRecordToInsertStatementWithRecoding(statement, record, compatInfo) }
    )

    private fun insertOrReplaceRecordBadgesAndCreateIdToOrdinalMap(
        appDatabase: AppDatabase,
        badges: Array<out RecordBadgeInfo>,
        progressReporter: ProgressReporter?
    ) = insertEntitiesAndCreateIdToOrdinalMap(
        appDatabase,
        badges,
        progressReporter,
        AppDatabase::compileInsertOrReplaceRecordBadgeStatement,
        SupportSQLiteStatement::bindRecordBadgeToInsertStatement
    )

    private fun createBadgeNameToIdMap(db: AppDatabase): BadgeNameToIdMap {
        return db.query("SELECT id, name FROM record_badges", null).use { c ->
            val count = c.count
            val map = BadgeNameToIdMap(count)

            for (i in 0 until count) {
                c.moveToPosition(i)

                val id = c.getInt(0)
                val name = c.getString(1)

                map.add(name, id)
            }

            map
        }
    }

    private fun insertPreserveRecordBadgesAndCreateIdToOrdinalMap(
        appDatabase: AppDatabase,
        badges: Array<out RecordBadgeInfo>,
        progressReporter: ProgressReporter?
    ): IdToOrdinalMap {
        var insertStatement: SupportSQLiteStatement? = null

        try {
            val nameToIdMap = createBadgeNameToIdMap(appDatabase)
            val idToOrdinalMap = IdToOrdinalMap(badges.size)

            for ((i, badge) in badges.withIndex()) {
                var badgeId = nameToIdMap.getIdByName(badge.name)

                if (badgeId < 0) {
                    if (insertStatement == null) {
                        insertStatement = appDatabase.compileInsertRecordBadgeStatement()
                    }

                    insertStatement.bindRecordBadgeToInsertStatement(badge)
                    badgeId = insertStatement.executeInsert().toInt()
                }

                idToOrdinalMap.add(badgeId)

                progressReporter?.onProgress(i + 1, badges.size)
            }

            return idToOrdinalMap
        } finally {
            try {
                insertStatement?.close()
            } catch (e: Exception) {
                // eat exception
            }
        }
    }

    private fun insertRecordToBadgeRelations(
        appDatabase: AppDatabase,
        entries: Array<out BackupBadgeToMultipleRecordEntry>,
        recordIdToOrdinalMap: IdToOrdinalMap,
        badgeIdToOrdinalMap: IdToOrdinalMap,
        progressReporter: ProgressReporter?
    ) {
        return appDatabase.compileInsertRecordToBadgeRelation().use { statement ->
            val totalSize = entries.sumOf { it.recordOrdinals.size }
            var seqIndex = 0

            trackProgressWith(progressReporter) {
                for ((badgeOrdinal, recordOrdinals) in entries) {
                    val badgeId = badgeIdToOrdinalMap.getIdByOrdinal(badgeOrdinal)

                    recordOrdinals.forEach { recordOrdinal ->
                        val recordId = recordIdToOrdinalMap.getIdByOrdinal(recordOrdinal)

                        statement.bindRecordToBadgeInsertStatement(recordId, badgeId)
                        statement.executeInsert()

                        seqIndex++
                        progressReporter?.onProgress(seqIndex, totalSize)
                    }
                }
            }
        }
    }
}