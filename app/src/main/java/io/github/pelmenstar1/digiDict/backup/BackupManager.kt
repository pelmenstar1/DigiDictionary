package io.github.pelmenstar1.digiDict.backup

import android.content.Context
import android.net.Uri
import androidx.sqlite.db.SupportSQLiteProgram
import androidx.sqlite.db.SupportSQLiteQuery
import androidx.sqlite.db.SupportSQLiteStatement
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

    fun createBackupData(
        appDatabase: AppDatabase,
        options: ExportOptions,
        progressReporter: ProgressReporter? = null
    ): BackupData {
        val records: Array<Record>
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

            BackupData(records, badges, badgeToMultipleRecordEntries)
        }
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

        return trackProgressWith(progressReporter) {
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
                val badgeToMultipleRecordEntries = data.badgeToMultipleRecordEntries

                if (options.importBadges && badges.isNotEmpty()) {
                    val replaceBadges = options.replaceBadges

                    val recordReporter = progressReporter?.subReporter(completed = 0, target = 33)
                    val badgeReporter = progressReporter?.subReporter(completed = 33, target = 67)
                    val recordToBadgeRelationReporter = progressReporter?.subReporter(completed = 67, target = 100)

                    val sortedRecordIds = insertRecordsAndSaveSortedIds(appDatabase, records, recordReporter)
                    val sortedBadgeIds = if (replaceBadges) {
                        insertOrReplaceRecordBadgesAndSaveSortedIds(appDatabase, badges, badgeReporter)
                    } else {
                        insertRecordBadgesAndSaveSortedIds(appDatabase, badges, badgeReporter)
                    }

                    insertRecordToBadgeRelations(
                        appDatabase,
                        badgeToMultipleRecordEntries,
                        sortedRecordIds,
                        sortedBadgeIds,
                        recordToBadgeRelationReporter
                    )
                } else {
                    appDatabase.compileInsertRecordStatement().use {
                        trackLoopProgressWith(progressReporter, records) { _, record ->
                            it.bindRecordToInsertStatement(record)
                            it.executeInsert()
                        }
                    }
                }
            }
        }
    }

    private inline fun <T> insertEntitiesAndSaveSortedIds(
        appDatabase: AppDatabase,
        entities: Array<out T>,
        progressReporter: ProgressReporter?,
        compileStatement: (AppDatabase) -> SupportSQLiteStatement,
        bind: (SupportSQLiteStatement, T) -> Unit
    ): IntArray {
        return compileStatement(appDatabase).use {
            val ids = IntArray(entities.size)

            trackLoopProgressWith(progressReporter, entities) { i, entity ->
                bind(it, entity)
                ids[i] = it.executeInsert().toInt()
            }

            Arrays.sort(ids)

            ids
        }
    }

    private fun insertRecordsAndSaveSortedIds(
        appDatabase: AppDatabase,
        records: Array<out Record>,
        progressReporter: ProgressReporter?
    ) = insertEntitiesAndSaveSortedIds(
        appDatabase,
        records,
        progressReporter,
        AppDatabase::compileInsertRecordStatement,
        SupportSQLiteStatement::bindRecordToInsertStatement
    )

    private fun insertOrReplaceRecordBadgesAndSaveSortedIds(
        appDatabase: AppDatabase,
        badges: Array<out RecordBadgeInfo>,
        progressReporter: ProgressReporter?
    ) = insertEntitiesAndSaveSortedIds(
        appDatabase,
        badges,
        progressReporter,
        AppDatabase::compileInsertOrReplaceRecordBadgeStatement,
        SupportSQLiteStatement::bindRecordBadgeToInsertStatement
    )

    private fun insertRecordBadgesAndSaveSortedIds(
        appDatabase: AppDatabase,
        badges: Array<out RecordBadgeInfo>,
        progressReporter: ProgressReporter?
    ): IntArray {
        var insertBadgeStatement: SupportSQLiteStatement? = null

        val getIdByNameQuery = object : SupportSQLiteQuery {
            private var name = ""

            override fun getSql() = "SELECT id FROM record_badges WHERE name=?"

            override fun bindTo(statement: SupportSQLiteProgram) {
                statement.bindString(1, name)
            }

            fun bindName(name: String) {
                this.name = name
            }

            override fun getArgCount() = 1
        }

        try {
            val ids = IntArray(badges.size)

            trackLoopProgressWith(progressReporter, badges.size) { i ->
                val badge = badges[i]

                getIdByNameQuery.bindName(badge.name)
                appDatabase.query(getIdByNameQuery).use {
                    if (it.count > 0) {
                        it.moveToPosition(0)
                        ids[i] = it.getInt(0)
                    } else {
                        var insertStatement = insertBadgeStatement

                        if (insertStatement == null) {
                            insertStatement = appDatabase.compileInsertRecordBadgeStatement()
                            insertBadgeStatement = insertStatement
                        }

                        insertStatement.bindRecordBadgeToInsertStatement(badge)
                        ids[i] = insertStatement.executeInsert().toInt()
                    }
                }
            }

            return ids
        } finally {
            try {
                insertBadgeStatement?.close()
            } catch (e: Exception) {
                // eat exception
            }
        }
    }

    private fun insertRecordToBadgeRelations(
        appDatabase: AppDatabase,
        entries: Array<out BackupBadgeToMultipleRecordEntry>,
        sortedRecordIds: IntArray,
        sortedBadgeIds: IntArray,
        progressReporter: ProgressReporter?
    ) {
        return appDatabase.compileInsertRecordToBadgeRelation().use { statement ->
            val totalSize = entries.sumOf { it.recordOrdinals.size }
            var seqIndex = 0

            trackProgressWith(progressReporter) {
                for ((badgeOrdinal, recordOrdinals) in entries) {
                    recordOrdinals.forEach { recordOrdinal ->
                        val recordId = sortedRecordIds[recordOrdinal]
                        val badgeId = sortedBadgeIds[badgeOrdinal]

                        statement.bindLong(1, recordId.toLong())
                        statement.bindLong(2, badgeId.toLong())
                        statement.executeInsert()

                        seqIndex++
                        progressReporter?.onProgress(seqIndex, totalSize)
                    }
                }
            }
        }
    }
}