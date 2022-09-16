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
import io.github.pelmenstar1.digiDict.common.queryArrayWithProgressReporter
import io.github.pelmenstar1.digiDict.common.trackLoopProgressWith
import io.github.pelmenstar1.digiDict.common.trackProgressWith
import io.github.pelmenstar1.digiDict.data.AppDatabase
import io.github.pelmenstar1.digiDict.data.Record
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
                records = getAllRecordsOrderByIdAsc(
                    appDatabase,
                    progressReporter?.subReporter(completed = 0f, target = .33f)
                )
                badges = getAllRecordBadgesOrderByIdAsc(
                    appDatabase,
                    progressReporter?.subReporter(completed = .33f, target = .67f)
                )
                val recordToBadgeRelations = getAllRecordToBadgeRelations(
                    appDatabase,
                    progressReporter?.subReporter(completed = .67f, target = 1f)
                )
                val recordIdToOrdinalMap = IdToOrdinalMap(records)

                badgeToMultipleRecordEntries = BackupHelpers.groupRecordToBadgeRelations(
                    recordToBadgeRelations,
                    recordIdToOrdinalMap
                )
            } else {
                records = getAllRecordsOrderByIdAsc(appDatabase, progressReporter)
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
            importer.import(input, options, progressReporter?.subReporter(completed = 0f, target = .9f)).also {
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
            try {
                val records = data.records
                val badges = data.badges
                val badgeToMultipleRecordEntries = data.badgeToMultipleRecordEntries

                if (options.importBadges && badges.isNotEmpty()) {
                    val replaceBadges = options.replaceBadges

                    val recordReporter = progressReporter?.subReporter(completed = 0f, target = .33f)
                    val badgeReporter = progressReporter?.subReporter(completed = .33f, target = .67f)
                    val recordToBadgeRelationReporter = progressReporter?.subReporter(completed = .67f, target = 1f)

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
                    compileInsertRecordStatement(appDatabase).use {
                        trackLoopProgressWith(progressReporter, records) { _, record ->
                            bindRecordToInsertStatement(it, record)
                            it.executeInsert()
                        }
                    }
                }
            } catch (th: Throwable) {
                progressReporter?.reportError()
                throw th
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
        this::compileInsertRecordStatement,
        this::bindRecordToInsertStatement
    )

    private fun insertOrReplaceRecordBadgesAndSaveSortedIds(
        appDatabase: AppDatabase,
        badges: Array<out RecordBadgeInfo>,
        progressReporter: ProgressReporter?
    ) = insertEntitiesAndSaveSortedIds(
        appDatabase,
        badges,
        progressReporter,
        this::compileInsertOrReplaceRecordBadgeStatement,
        this::bindRecordBadgeToInsertStatement
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
                            insertStatement = compileInsertRecordBadgeStatement(appDatabase)
                            insertBadgeStatement = insertStatement
                        }

                        bindRecordBadgeToInsertStatement(insertStatement, badge)
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
        return compileInsertRecordToBadgeRelation(appDatabase).use { statement ->
            val totalSize = entries.sumOf { it.recordOrdinals.size }
            val fTotalSize = totalSize.toFloat()
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
                        progressReporter?.onProgress(seqIndex / fTotalSize)
                    }
                }
            }
        }
    }

    private fun compileInsertRecordStatement(appDatabase: AppDatabase): SupportSQLiteStatement {
        return appDatabase.compileStatement("INSERT OR REPLACE INTO records (expression, meaning, additionalNotes, score, dateTime) VALUES (?,?,?,?,?)")
    }

    private fun bindRecordToInsertStatement(statement: SupportSQLiteStatement, record: Record) {
        statement.also {
            it.bindString(1, record.expression)
            it.bindString(2, record.meaning)
            it.bindString(3, record.additionalNotes)
            it.bindLong(4, record.score.toLong())
            it.bindLong(5, record.epochSeconds)
        }
    }

    private fun compileInsertOrReplaceRecordBadgeStatement(appDatabase: AppDatabase): SupportSQLiteStatement {
        return appDatabase.compileStatement("INSERT OR REPLACE INTO record_badges (name, outlineColor) VALUES (?,?)")
    }

    private fun compileInsertRecordBadgeStatement(appDatabase: AppDatabase): SupportSQLiteStatement {
        return appDatabase.compileStatement("INSERT INTO record_badges (name, outlineColor) VALUES (?,?)")
    }

    private fun bindRecordBadgeToInsertStatement(statement: SupportSQLiteStatement, badge: RecordBadgeInfo) {
        statement.also {
            it.bindString(1, badge.name)
            it.bindLong(2, badge.outlineColor.toLong())
        }
    }

    private fun compileInsertRecordToBadgeRelation(appDatabase: AppDatabase): SupportSQLiteStatement {
        return appDatabase.compileStatement("INSERT INTO record_to_badge_relations (recordId, badgeId) VALUES (?, ?)")
    }

    private fun compileGetBadgeIdByName(appDatabase: AppDatabase): SupportSQLiteStatement {
        return appDatabase.compileStatement("SELECT id FROM record_badges WHERE name=?")
    }

    private fun getAllRecordsOrderByIdAsc(
        appDatabase: AppDatabase,
        progressReporter: ProgressReporter?
    ): Array<Record> {
        return appDatabase.queryArrayWithProgressReporter(
            "SELECT id, expression, meaning, additionalNotes, score, dateTime FROM records ORDER BY id ASC",
            null,
            progressReporter
        ) { c ->
            val id = c.getInt(0)
            val expr = c.getString(1)
            val meaning = c.getString(2)
            val notes = c.getString(3)
            val score = c.getInt(4)
            val epochSeconds = c.getLong(5)

            Record(id, expr, meaning, notes, score, epochSeconds)
        }
    }

    private fun getAllRecordBadgesOrderByIdAsc(
        appDatabase: AppDatabase,
        progressReporter: ProgressReporter?
    ): Array<RecordBadgeInfo> {
        return appDatabase.queryArrayWithProgressReporter(
            "SELECT id, name, outlineColor FROM record_badges ORDER BY id ASC",
            null,
            progressReporter
        ) { c ->
            val id = c.getInt(0)
            val name = c.getString(1)
            val outlineColor = c.getInt(2)

            RecordBadgeInfo(id, name, outlineColor)
        }
    }

    private fun getAllRecordToBadgeRelations(
        appDatabase: AppDatabase,
        progressReporter: ProgressReporter?
    ): Array<RecordToBadgeRelation> {
        return appDatabase.queryArrayWithProgressReporter(
            "SELECT recordId, badgeId FROM record_to_badge_relations ORDER BY badgeId ASC",
            null,
            progressReporter
        ) { c ->
            val recordId = c.getInt(0)
            val badgeId = c.getInt(1)

            // relationId is not used anywhere, so let it be zero.
            RecordToBadgeRelation(0, recordId, badgeId)
        }
    }
}