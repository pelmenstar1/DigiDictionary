package io.github.pelmenstar1.digiDict

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.pelmenstar1.digiDict.backup.BackupCompatInfo
import io.github.pelmenstar1.digiDict.backup.BackupFormat
import io.github.pelmenstar1.digiDict.backup.BackupManager
import io.github.pelmenstar1.digiDict.backup.exporting.ExportOptions
import io.github.pelmenstar1.digiDict.backup.importing.ImportOptions
import io.github.pelmenstar1.digiDict.common.unsafeNewArray
import io.github.pelmenstar1.digiDict.data.*
import io.github.pelmenstar1.digiDict.utils.assertContentEqualsNoId
import io.github.pelmenstar1.digiDict.utils.useInMemoryDb
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.io.RandomAccessFile
import kotlin.test.assertEquals

@RunWith(AndroidJUnit4::class)
class BackupManagerTests {
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val recordCounts = intArrayOf(1, 4, 16, 128, 1024)
    private val badgeCounts = intArrayOf(1, 2, 4, 8)

    private val dddbVersions = intArrayOf(0, 1)
    private val jsonVersions = intArrayOf(0, 1)

    private fun backupFile(format: BackupFormat): File {
        return context.getFileStreamPath("test.${format.extension}").also {
            // Truncate the file.
            RandomAccessFile(it, "rw").setLength(0L)
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

    private fun File.export(
        db: AppDatabase,
        options: ExportOptions,
        format: BackupFormat,
        version: Int,
        compatInfo: BackupCompatInfo = BackupManager.latestCompatInfo
    ) {
        outputStream().use {
            BackupManager.export(it, BackupManager.createBackupData(db, options, compatInfo), format, version)
        }
    }

    private fun File.importAndDeploy(db: AppDatabase, options: ImportOptions, format: BackupFormat) {
        val data = inputStream().use {
            BackupManager.import(it, format, options)
        }

        BackupManager.deployImportData(data, options, db)
    }

    private fun roundtripOnlyRecordsTestHelper(
        format: BackupFormat,
        size: Int,
        exportVersion: Int,
        compatInfo: BackupCompatInfo = BackupManager.latestCompatInfo
    ) = useInMemoryDb(context) { db ->
        val file = backupFile(format)

        val records = insertAndGetRecords(db, size)

        file.export(db, ExportOptions(exportBadges = false), format, exportVersion, compatInfo)

        db.clearAllTables()
        file.importAndDeploy(db, ImportOptions(importBadges = false, replaceBadges = false), format)

        val importedRecordsInDb = getAllRecordsNoIdInSet(db)

        assertEquals(records, importedRecordsInDb)
    }

    private suspend fun roundtripWithBadgesPreserve(
        format: BackupFormat,
        recordCount: Int,
        badgeCount: Int,
        exportVersion: Int,
        compatInfo: BackupCompatInfo = BackupManager.latestCompatInfo
    ) = useInMemoryDb(context) { db ->
        val file = backupFile(format)

        val badgeDao = db.recordBadgeDao()

        val recordsNoIdSet = insertAndGetRecords(db, recordCount)

        insertBadges(db, badgeCount)
        insertBadgeRelations(db)

        file.export(db, ExportOptions(exportBadges = true), format, exportVersion, compatInfo)

        db.clearAllTables()

        val noIdNewBadges = createNoIdBadges(badgeCount, colorAddition = 1)
        badgeDao.insertAll(noIdNewBadges)

        file.importAndDeploy(db, ImportOptions(importBadges = true, replaceBadges = false), format)

        val importedRecordsInDb = getAllRecordsNoIdInSet(db)
        val importedBadges = badgeDao.getAllOrderByIdAsc()

        assertEquals(recordsNoIdSet, importedRecordsInDb)

        // Check if badges are changed when we're in "preserve" mode (replaceBadges = false)
        assertContentEqualsNoId(noIdNewBadges, importedBadges)

        validateBadgeRelations(db, importedBadges)
    }

    private suspend fun roundtripWithBadgesReplace(
        format: BackupFormat,
        recordCount: Int,
        badgeCount: Int,
        exportVersion: Int
    ) = useInMemoryDb(context) { db ->
        val file = backupFile(format)

        val badgeDao = db.recordBadgeDao()

        val recordsNoIdSet = insertAndGetRecords(db, recordCount)
        insertBadges(db, badgeCount)

        val badgesToExport = badgeDao.getAllOrderByIdAsc()
        insertBadgeRelations(db, badgesToExport)

        file.export(db, ExportOptions(exportBadges = true), format, exportVersion)

        db.clearAllTables()
        insertBadges(db, badgeCount, colorAddition = 1)

        file.importAndDeploy(db, ImportOptions(importBadges = true, replaceBadges = true), format)

        val importedRecordsInDb = getAllRecordsNoIdInSet(db)
        val importedBadges = badgeDao.getAllOrderByIdAsc()

        assertEquals(recordsNoIdSet, importedRecordsInDb)
        assertContentEqualsNoId(badgesToExport, importedBadges)
        validateBadgeRelations(db, importedBadges)
    }


    @Test
    fun roundtripOnlyRecords_dddb() {
        for (version in dddbVersions) {
            for (recordCount in recordCounts) {
                roundtripOnlyRecordsTestHelper(BackupFormat.DDDB, recordCount, version)
            }
        }
    }

    @Test
    fun roundtripOnlyRecords_json() {
        for (version in jsonVersions) {
            for (recordCount in recordCounts) {
                roundtripOnlyRecordsTestHelper(BackupFormat.JSON, recordCount, version)
            }
        }
    }

    @Test
    fun roundtripOnlyRecords_fromOldMeaningFormatToNew_dddb() {
        val compatInfo = BackupCompatInfo(newMeaningFormat = false)

        for (version in dddbVersions) {
            roundtripOnlyRecordsTestHelper(BackupFormat.DDDB, size = 16, version, compatInfo)
        }
    }

    // The logic of inserting records a little bit different when badges are available.
    @Test
    fun roundtripWithBadges_fromOldMeaningFormatToNew_dddb() = runTest {
        val compatInfo = BackupCompatInfo(newMeaningFormat = false)

        for (version in dddbVersions) {
            roundtripWithBadgesPreserve(BackupFormat.DDDB, recordCount = 16, badgeCount = 2, version, compatInfo)
        }
    }

    // Also test with JSON format to check whether compatibility info is processed correctly.
    @Test
    fun roundtripOnlyRecords_fromOldMeaningFormatToNew_json() {
        val compatInfo = BackupCompatInfo(newMeaningFormat = false)

        for (version in jsonVersions) {
            roundtripOnlyRecordsTestHelper(BackupFormat.JSON, size = 16, version, compatInfo)
        }
    }

    // The logic of inserting records a little bit different when badges are available.
    @Test
    fun roundtripWithBadges_fromOldMeaningFormatToNew_json() = runTest {
        val compatInfo = BackupCompatInfo(newMeaningFormat = false)

        for (version in jsonVersions) {
            roundtripWithBadgesPreserve(BackupFormat.JSON, recordCount = 16, badgeCount = 2, version, compatInfo)
        }
    }

    @Test
    fun roundtripWithBadges_dddb_preserve() = runTest {
        for (version in dddbVersions) {
            for (recordCount in recordCounts) {
                for (badgeCount in badgeCounts) {
                    roundtripWithBadgesPreserve(BackupFormat.DDDB, recordCount, badgeCount, version)
                }
            }
        }
    }

    @Test
    fun roundtripWithBadges_json_preserve() = runTest {
        for (version in jsonVersions) {
            for (recordCount in recordCounts) {
                for (badgeCount in badgeCounts) {
                    roundtripWithBadgesPreserve(BackupFormat.JSON, recordCount, badgeCount, version)
                }
            }
        }
    }

    @Test
    fun roundtripWithBadges_dddb_replace() = runTest {
        for (version in dddbVersions) {
            for (recordCount in recordCounts) {
                for (badgeCount in badgeCounts) {
                    roundtripWithBadgesReplace(BackupFormat.DDDB, recordCount, badgeCount, version)
                }
            }
        }
    }

    @Test
    fun roundtripWithBadges_json_replace() = runTest {
        for (version in jsonVersions) {
            for (recordCount in recordCounts) {
                for (badgeCount in badgeCounts) {
                    roundtripWithBadgesReplace(BackupFormat.JSON, recordCount, badgeCount, version)
                }
            }
        }
    }

    private fun getAllBadgeIds(db: AppDatabase): IntArray {
        return db.query("SELECT id FROM record_badges", null).use { c ->
            val count = c.count
            val array = IntArray(count)

            for (i in 0 until count) {
                c.moveToPosition(i)

                array[i] = c.getInt(0)
            }

            array
        }
    }

    private inline fun forEachId(db: AppDatabase, query: String, block: (Int) -> Unit) {
        return db.query(query, null).use { c ->
            val count = c.count

            for (i in 0 until count) {
                c.moveToPosition(i)

                block(c.getInt(0))
            }
        }
    }

    private inline fun forEachRecordId(db: AppDatabase, block: (Int) -> Unit) {
        forEachId(db, "SELECT id FROM records", block)
    }

    private inline fun forEachRecordIdAsc(db: AppDatabase, block: (Int) -> Unit) {
        forEachId(db, "SELECT id FROM records ORDER BY id ASC", block)
    }

    private fun getAllRecordsNoIdInSet(db: AppDatabase): Set<RecordNoId> {
        return db.query(
            "SELECT expression, meaning, additionalNotes, score, dateTime FROM records",
            null
        ).use { c ->
            val count = c.count
            val set = HashSet<RecordNoId>(count, 1f)

            for (i in 0 until count) {
                c.moveToPosition(i)

                val expr = c.getString(0)
                val meaning = c.getString(1)
                val notes = c.getString(2)
                val score = c.getInt(3)
                val epochSeconds = c.getLong(4)

                set.add(RecordNoId(expr, meaning, notes, score, epochSeconds))
            }

            set
        }
    }

    private fun getAllRecordToBadgeRelationsOrderByBadgeAndRecordIds(db: AppDatabase): Array<RecordToBadgeRelation> {
        return db.query(
            "SELECT recordId, badgeId FROM record_to_badge_relations ORDER BY recordId ASC, badgeId ASC",
            null
        ).use { c ->
            val count = c.count
            val result = unsafeNewArray<RecordToBadgeRelation>(count)

            for (i in 0 until count) {
                c.moveToPosition(i)

                val badgeId = c.getInt(0)
                val recordId = c.getInt(1)

                // Relation id is not used in tests
                result[i] = RecordToBadgeRelation(relationId = 0, badgeId, recordId)
            }

            result
        }
    }

    private fun validateBadgeRelations(
        db: AppDatabase,
        sortedBadges: Array<out RecordBadgeInfo>
    ) {
        val relations = getAllRecordToBadgeRelationsOrderByBadgeAndRecordIds(db)
        var relationIndex = 0

        forEachRecordIdAsc(db) { recordId ->
            for (badge in sortedBadges) {
                val badgeId = badge.id
                val relation = relations[relationIndex++]

                val relationRecordId = relation.recordId
                val relationBadgeId = relation.badgeId

                assertEquals(recordId, relationRecordId, "mismatched record id")
                assertEquals(badgeId, relationBadgeId, "mismatched badge id")
            }
        }
    }

    private fun insertAndGetRecords(db: AppDatabase, size: Int): Set<RecordNoId> {
        return db.compileInsertRecordStatement().use { insertStatement ->
            val result = HashSet<RecordNoId>(size, 1f)

            for (i in 0 until size) {
                val expr = "Expression$i"
                val meaning = if (i % 2 == 0) {
                    "CMeaning$i"
                } else {
                    "L2@Meaning_1_${i}${ComplexMeaning.LIST_NEW_ELEMENT_SEPARATOR}Meaning_2_${i}"
                }

                val additionalNotes = "Notes$i"
                val epochSeconds = i * 1000L

                insertStatement.bindRecordToInsertStatement(expr, meaning, additionalNotes, score = i, epochSeconds)
                insertStatement.executeInsert()

                result.add(RecordNoId(expr, meaning, additionalNotes, score = i, epochSeconds))
            }

            result
        }
    }

    private fun insertBadges(db: AppDatabase, size: Int, colorAddition: Int = 0) {
        db.compileInsertRecordBadgeStatement().use { insertStatement ->
            for (i in 0 until size) {
                val name = "Badge$i"
                val outlineColor = i + colorAddition

                insertStatement.bindRecordBadgeToInsertStatement(name, outlineColor)
                insertStatement.executeInsert()
            }
        }
    }

    private fun insertBadgeRelations(db: AppDatabase) {
        val badgeIds = getAllBadgeIds(db)

        db.compileInsertRecordToBadgeRelation().use { insertStatement ->
            forEachRecordId(db) { recordId ->
                for (badgeId in badgeIds) {
                    insertStatement.bindRecordToBadgeInsertStatement(recordId, badgeId)

                    insertStatement.executeInsert()
                }
            }
        }
    }

    private fun insertBadgeRelations(db: AppDatabase, badges: Array<out RecordBadgeInfo>) {
        db.compileInsertRecordToBadgeRelation().use { insertStatement ->
            forEachRecordId(db) { recordId ->
                for (badge in badges) {
                    insertStatement.bindRecordToBadgeInsertStatement(recordId, badge.id)

                    insertStatement.executeInsert()
                }
            }
        }
    }
}