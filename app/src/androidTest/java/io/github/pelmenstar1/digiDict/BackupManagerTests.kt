package io.github.pelmenstar1.digiDict

import android.content.Context
import android.database.Cursor
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.pelmenstar1.digiDict.backup.BackupFormat
import io.github.pelmenstar1.digiDict.backup.BackupManager
import io.github.pelmenstar1.digiDict.backup.exporting.ExportOptions
import io.github.pelmenstar1.digiDict.backup.importing.ImportOptions
import io.github.pelmenstar1.digiDict.data.*
import io.github.pelmenstar1.digiDict.utils.AppDatabaseUtils
import io.github.pelmenstar1.digiDict.utils.assertContentEqualsNoId
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import kotlin.test.assertEquals

@RunWith(AndroidJUnit4::class)
class BackupManagerTests {
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val recordCounts = intArrayOf(1, 4, 16, 128, 1024)
    private val badgeCounts = intArrayOf(1, 2, 4, 16)

    private fun backupFile(format: BackupFormat): File {
        return context.getFileStreamPath("test.${format.extension}").also {
            it.delete()
            it.createNewFile()
        }
    }

    private fun createNoIdRecords(size: Int): Array<Record> {
        return Array(size) { i ->
            Record(
                id = 0,
                expression = "Expression$i",
                meaning = "CMeaning$i",
                additionalNotes = "Notes$i",
                score = i,
                epochSeconds = i * 1000L
            )
        }
    }

    private fun createNoIdBadges(size: Int, colorAddition: Int = 0): Array<RecordBadgeInfo> {
        return Array(size) { i ->
            RecordBadgeInfo(
                id = 0,
                name = "Badge$i",
                outlineColor = i + colorAddition
            )
        }
    }

    private fun File.export(db: AppDatabase, options: ExportOptions, format: BackupFormat) {
        outputStream().use {
            BackupManager.export(it, BackupManager.createBackupData(db, options), format)
        }
    }

    private fun File.importAndDeploy(db: AppDatabase, options: ImportOptions, format: BackupFormat) {
        val data = inputStream().use {
            BackupManager.import(it, format, options)
        }

        BackupManager.deployImportData(data, options, db)
    }

    private fun roundtripOnlyRecordsTestHelper(format: BackupFormat, size: Int) = runTest {
        val db = AppDatabaseUtils.createTestDatabase(context)
        try {
            val file = backupFile(format)
            val recordDao = db.recordDao()

            val noIdRecords = createNoIdRecords(size)

            recordDao.insertAll(noIdRecords)
            val records = getAllRecordsNoIdInSet(recordDao)

            file.export(db, ExportOptions(exportBadges = false), format)

            db.clearAllTables()
            file.importAndDeploy(db, ImportOptions(importBadges = false, replaceBadges = false), format)

            val importedRecordsInDb = getAllRecordsNoIdInSet(recordDao)

            assertEquals(records, importedRecordsInDb)
        } finally {
            db.close()
        }
    }

    private fun roundtripWithBadgesPreserve(
        format: BackupFormat,
        recordCount: Int,
        badgeCount: Int
    ) = runTest {
        val db = AppDatabaseUtils.createTestDatabase(context)
        try {
            val file = backupFile(format)

            val recordDao = db.recordDao()
            val badgeDao = db.recordBadgeDao()
            val recordToBadgeRelationsDao = db.recordToBadgeRelationDao()

            val noIdRecords = createNoIdRecords(recordCount)
            val noIdBadges = createNoIdBadges(badgeCount)

            recordDao.insertAll(noIdRecords)
            badgeDao.insertAll(noIdBadges)

            val records = recordDao.getAllRecords()
            val recordsNoIdSet = getAllRecordsNoIdInSet(recordDao)

            val badgesToExport = badgeDao.getAllOrderByIdAsc()
            val recordToBadgeRelations = buildList(records.size * badgesToExport.size) {
                for (record in records) {
                    for (badge in badgesToExport) {
                        add(RecordToBadgeRelation(0, record.id, badge.id))
                    }
                }
            }.toTypedArray()

            recordToBadgeRelationsDao.insertAll(recordToBadgeRelations)

            file.export(db, ExportOptions(exportBadges = true), format)

            db.clearAllTables()
            val noIdNewBadges = createNoIdBadges(badgeCount, colorAddition = 1)

            badgeDao.insertAll(noIdNewBadges)

            file.importAndDeploy(db, ImportOptions(importBadges = true, replaceBadges = false), format)

            val importedRecordsInDb = getAllRecordsNoIdInSet(recordDao)
            val importedBadges = badgeDao.getAllOrderByIdAsc()

            assertEquals(recordsNoIdSet, importedRecordsInDb)

            // Check if badges are changed when we're in "preserve" mode (replaceBadges = false)
            assertContentEqualsNoId(noIdNewBadges, importedBadges)

            repeat(badgeCount) {
                val badgeId = importedBadges[it].id
                val recordsRelatedToBadge = recordToBadgeRelationsDao.getByBadgeId(badgeId)

                assertEquals(recordCount, recordsRelatedToBadge.size)
            }
        } finally {
            db.close()
        }
    }

    private fun roundtripWithBadgesReplace(
        format: BackupFormat,
        recordCount: Int,
        badgeCount: Int
    ) = runTest {
        val db = AppDatabaseUtils.createTestDatabase(context)
        try {
            val file = backupFile(format)

            val recordDao = db.recordDao()
            val badgeDao = db.recordBadgeDao()
            val recordToBadgeRelationsDao = db.recordToBadgeRelationDao()

            val noIdRecords = createNoIdRecords(recordCount)
            val noIdBadges = createNoIdBadges(badgeCount)

            recordDao.insertAll(noIdRecords)
            badgeDao.insertAll(noIdBadges)

            val records = recordDao.getAllRecords()
            val recordsNoIdSet = getAllRecordsNoIdInSet(recordDao)

            val badgesToExport = badgeDao.getAllOrderByIdAsc()
            val recordToBadgeRelations = buildList(records.size * badgesToExport.size) {
                for (record in records) {
                    for (badge in badgesToExport) {
                        add(RecordToBadgeRelation(0, record.id, badge.id))
                    }
                }
            }.toTypedArray()

            recordToBadgeRelationsDao.insertAll(recordToBadgeRelations)

            file.export(db, ExportOptions(exportBadges = true), format)

            db.clearAllTables()
            val noIdOldBadges = createNoIdBadges(badgeCount, colorAddition = 1)
            badgeDao.insertAll(noIdOldBadges)

            file.importAndDeploy(db, ImportOptions(importBadges = true, replaceBadges = true), format)

            val importedRecordsInDb = getAllRecordsNoIdInSet(recordDao)
            val importedBadges = badgeDao.getAllOrderByIdAsc()

            assertEquals(recordsNoIdSet, importedRecordsInDb)
            assertContentEqualsNoId(badgesToExport, importedBadges)

            repeat(badgeCount) {
                val badgeId = importedBadges[it].id
                val recordsRelatedToBadge = recordToBadgeRelationsDao.getByBadgeId(badgeId)

                assertEquals(recordCount, recordsRelatedToBadge.size)
            }
        } finally {
            db.close()
        }
    }

    @Test
    fun roundtripOnlyRecords_dddb() {
        for (recordCount in recordCounts) {
            roundtripOnlyRecordsTestHelper(BackupFormat.DDDB, recordCount)
        }
    }

    @Test
    fun roundtripOnlyRecords_json() {
        for (recordCount in recordCounts) {
            roundtripOnlyRecordsTestHelper(BackupFormat.JSON, recordCount)
        }
    }

    @Test
    fun roundtripWithBadges_dddb_preserve() {
        for (recordCount in recordCounts) {
            for (badgeCount in badgeCounts) {
                roundtripWithBadgesPreserve(BackupFormat.DDDB, recordCount, badgeCount)
            }
        }
    }

    @Test
    fun roundtripWithBadges_json_preserve() {
        for (recordCount in recordCounts) {
            for (badgeCount in badgeCounts) {
                roundtripWithBadgesPreserve(BackupFormat.JSON, recordCount, badgeCount)
            }
        }
    }

    @Test
    fun roundtripWithBadges_dddb_replace() {
        for (recordCount in recordCounts) {
            for (badgeCount in badgeCounts) {
                roundtripWithBadgesReplace(BackupFormat.DDDB, recordCount, badgeCount)
            }
        }
    }

    @Test
    fun roundtripWithBadges_json_replace() {
        for (recordCount in recordCounts) {
            for (badgeCount in badgeCounts) {
                roundtripWithBadgesReplace(BackupFormat.JSON, recordCount, badgeCount)
            }
        }
    }

    companion object {
        internal fun getAllRecordsNoIdInSet(dao: RecordDao): Set<RecordNoId> {
            return dao.getAllRecordsNoIdRaw().use { cursor ->
                val exprIndex = cursor.getColumnIndex { expression }
                val meaningIndex = cursor.getColumnIndex { meaning }
                val notesIndex = cursor.getColumnIndex { additionalNotes }
                val scoreIndex = cursor.getColumnIndex { score }
                val epochSecondsIndex = cursor.getColumnIndex { epochSeconds }

                val count = cursor.count
                val set = HashSet<RecordNoId>(count)

                while (cursor.moveToNext()) {
                    val expr = cursor.getString(exprIndex)
                    val meaning = cursor.getString(meaningIndex)
                    val notes = cursor.getString(notesIndex)
                    val score = cursor.getInt(scoreIndex)
                    val epochSeconds = cursor.getLong(epochSecondsIndex)

                    set.add(RecordNoId(expr, meaning, notes, score, epochSeconds))
                }

                set
            }
        }

        private inline fun Cursor.getColumnIndex(columnName: RecordTable.() -> String): Int {
            return getColumnIndex(RecordTable.columnName())
        }
    }
}