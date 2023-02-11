package io.github.pelmenstar1.digiDict.db

import androidx.sqlite.db.SupportSQLiteStatement
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import io.github.pelmenstar1.digiDict.data.*
import io.github.pelmenstar1.digiDict.utils.AppDatabaseUtils
import io.github.pelmenstar1.digiDict.utils.assertContentEqualsNoId
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals

@RunWith(AndroidJUnit4::class)
class AppDatabaseManualQueriesTests {
    private val context = InstrumentationRegistry.getInstrumentation().context

    private fun createEmptyDatabase(): AppDatabase {
        return AppDatabaseUtils.createTestDatabase(context).also {
            it.clearAllTables()
        }
    }

    private fun assertContentEqualsNoId(
        expected: Array<out RecordToBadgeRelation>,
        actual: Array<out RecordToBadgeRelation>
    ) {
        assertContentEqualsNoId(expected, actual) { other -> equalsNoId(other) }
    }

    private fun AppDatabase.insertRecordsAndBadges(
        records: Array<out Record>,
        badges: Array<out RecordBadgeInfo>,
        relations: Array<out RecordToBadgeRelation>
    ) {
        runBlocking {
            recordDao().insertAll(records)
            recordBadgeDao().insertAll(badges)
            recordToBadgeRelationDao().insertAll(relations)
        }
    }

    @Test
    fun compileInsertRecordStatementTest() {
        val db = createEmptyDatabase()
        val statement = db.compileInsertRecordStatement()

        val records = arrayOf(
            Record(
                expression = "123",
                meaning = "CMeaning1",
                additionalNotes = "1",
                score = 123,
                epochSeconds = 100L
            ),
            Record(
                expression = "322",
                meaning = "CMeaning2",
                additionalNotes = "2",
                score = 222,
                epochSeconds = 111L
            )
        )

        for (record in records) {
            // Also testing bindRecordToInsertStatement.
            // We can't test it directly as bind data are inaccessible
            statement.bindRecordToInsertStatement(record)

            statement.executeInsert()
        }

        val actualRecords = db.getAllRecordsOrderByIdAsc(progressReporter = null)

        assertContentEqualsNoId(records, actualRecords)
    }

    private fun insertRecordBadgeStatementTestHelper(getStatement: AppDatabase.() -> SupportSQLiteStatement) {
        val db = createEmptyDatabase()
        val statement = db.getStatement()
        val badges = arrayOf(
            RecordBadgeInfo(name = "1", outlineColor = 123),
            RecordBadgeInfo(name = "2", outlineColor = 321)
        )

        for (badge in badges) {
            // Also testing bindRecordBadgeToInsertStatement.
            // We can't test it directly as bind data are inaccessible
            statement.bindRecordBadgeToInsertStatement(badge)
            statement.executeInsert()
        }

        val actualBadges = db.getAllRecordBadgesOrderByIdAsc(progressReporter = null)

        assertContentEqualsNoId(badges, actualBadges)
    }

    @Test
    fun compileInsertRecordBadgeStatementTest() {
        insertRecordBadgeStatementTestHelper { compileInsertRecordBadgeStatement() }
    }

    @Test
    fun compileInsertOrReplaceRecordBadgeStatementInsertModeTest() {
        insertRecordBadgeStatementTestHelper { compileInsertOrReplaceRecordBadgeStatement() }
    }

    @Test
    fun compileInsertOrReplaceRecordBadgeStatementReplaceModeTest() {
        val db = createEmptyDatabase()
        val statement = db.compileInsertOrReplaceRecordBadgeStatement()
        val badges = arrayOf(
            RecordBadgeInfo(name = "1", outlineColor = 123),
            RecordBadgeInfo(name = "2", outlineColor = 321)
        )

        runBlocking {
            db.recordBadgeDao().insert(RecordBadgeInfo(name = "1", outlineColor = 122))
        }

        for (badge in badges) {
            // Also testing bindRecordBadgeToInsertStatement.
            // We can't test it directly as bind data are inaccessible
            statement.bindRecordBadgeToInsertStatement(badge)
            statement.executeInsert()
        }

        val actualBadges = db.getAllRecordBadgesOrderByIdAsc(progressReporter = null)

        assertContentEqualsNoId(badges, actualBadges)
    }

    @Test
    fun compileInsertRecordToBadgeRelationTest() {
        val db = createEmptyDatabase()
        val statement = db.compileInsertRecordToBadgeRelation()

        val relations = arrayOf(
            RecordToBadgeRelation(recordId = 1, badgeId = 2),
            RecordToBadgeRelation(recordId = 2, badgeId = 3),
            RecordToBadgeRelation(recordId = 3, badgeId = 4)
        )

        for (relation in relations) {
            statement.bindRecordToBadgeInsertStatement(relation)
            statement.executeInsert()
        }

        val actualRelations = db.getAllRecordToBadgeRelations(progressReporter = null)
        assertContentEqualsNoId(relations, actualRelations)
    }

    @Test
    fun getAllRecordsOrderByIdAscTest() {
        val db = createEmptyDatabase()

        val records = arrayOf(
            Record(
                expression = "123",
                meaning = "CMeaning1",
                additionalNotes = "1",
                score = 123,
                epochSeconds = 100L
            ),
            Record(
                expression = "322",
                meaning = "CMeaning2",
                additionalNotes = "2",
                score = 222,
                epochSeconds = 111L
            )
        )

        runBlocking {
            db.recordDao().insertAll(records)
        }

        val actualRecords = db.getAllRecordsOrderByIdAsc(progressReporter = null)

        assertContentEqualsNoId(records, actualRecords)
    }

    @Test
    fun getAllRecordBadgesOrderByIdAscTest() {
        val db = createEmptyDatabase()

        val badges = arrayOf(
            RecordBadgeInfo(name = "1", outlineColor = 123),
            RecordBadgeInfo(name = "2", outlineColor = 321)
        )

        runBlocking {
            db.recordBadgeDao().insertAll(badges)
        }

        val actualBadges = db.getAllRecordBadgesOrderByIdAsc(progressReporter = null)
        assertContentEqualsNoId(badges, actualBadges)
    }

    @Test
    fun getAllRecordToBadgeRelationsTest() {
        val db = createEmptyDatabase()

        val relations = arrayOf(
            RecordToBadgeRelation(recordId = 1, badgeId = 2),
            RecordToBadgeRelation(recordId = 2, badgeId = 3),
            RecordToBadgeRelation(recordId = 3, badgeId = 4)
        )

        runBlocking {
            db.recordToBadgeRelationDao().insertAll(relations)
        }

        val actualRelations = db.getAllRecordToBadgeRelations(progressReporter = null)
        assertContentEqualsNoId(relations, actualRelations)
    }

    @Test
    fun getBadgesByRecordIdTest() {
        val db = createEmptyDatabase()

        val badges = arrayOf(
            RecordBadgeInfo(name = "Badge1", outlineColor = 123),
            RecordBadgeInfo(name = "Badge2", outlineColor = 124),
            RecordBadgeInfo(name = "Badge3", outlineColor = 125)
        )

        val recordId1Badges = arrayOf(
            RecordBadgeInfo(name = "Badge1", outlineColor = 123),
            RecordBadgeInfo(name = "Badge2", outlineColor = 124)
        )

        val relations = arrayOf(
            RecordToBadgeRelation(recordId = 1, badgeId = 1),
            RecordToBadgeRelation(recordId = 1, badgeId = 2)
        )

        runBlocking {
            db.recordBadgeDao().insertAll(badges)
            db.recordToBadgeRelationDao().insertAll(relations)
        }

        val query = GetBadgesByRecordIdQuery()
        val actualRecordIdBadges = db.getBadgesByRecordId(query, recordId = 1)

        assertContentEqualsNoId(recordId1Badges, actualRecordIdBadges)
    }

    @Test
    fun getAllConciseRecordsWithBadgesTest() {
        val db = createEmptyDatabase()

        val records = arrayOf(
            Record(
                expression = "E1",
                meaning = "CMeaning1",
                additionalNotes = "1",
                score = 111,
                epochSeconds = 1111L
            ),
            Record(
                expression = "E2",
                meaning = "CMeaning2",
                additionalNotes = "2",
                score = 222,
                epochSeconds = 2222L
            ),
            Record(
                expression = "E3",
                meaning = "CMeaning2",
                additionalNotes = "3",
                score = 333,
                epochSeconds = 3333L
            ),
        )

        val badges = arrayOf(
            RecordBadgeInfo(name = "Badge1", outlineColor = 123),
            RecordBadgeInfo(name = "Badge2", outlineColor = 124),
            RecordBadgeInfo(name = "Badge3", outlineColor = 125)
        )

        val relations = arrayOf(
            RecordToBadgeRelation(recordId = 1, badgeId = 1),
            RecordToBadgeRelation(recordId = 1, badgeId = 2),
            RecordToBadgeRelation(recordId = 2, badgeId = 3)
        )

        db.insertRecordsAndBadges(records, badges, relations)

        val actualConciseRecords = db.getAllConciseRecordsWithBadges(progressReporter = null)
        val expectedConciseRecords = arrayOf(
            ConciseRecordWithBadges(
                id = 1,
                expression = "E1",
                meaning = "CMeaning1",
                score = 111,
                epochSeconds = 1111L,
                badges = arrayOf(
                    RecordBadgeInfo(id = 1, name = "Badge1", outlineColor = 123),
                    RecordBadgeInfo(id = 2, name = "Badge2", outlineColor = 124)
                )
            ),
            ConciseRecordWithBadges(
                id = 2,
                expression = "E2",
                meaning = "CMeaning2",
                score = 222,
                epochSeconds = 2222L,
                badges = arrayOf(
                    RecordBadgeInfo(id = 3, name = "Badge3", outlineColor = 125)
                )
            ),
            ConciseRecordWithBadges(
                id = 3,
                expression = "E3",
                meaning = "CMeaning2",
                score = 333,
                epochSeconds = 3333L,
                badges = emptyArray()
            )
        )

        assertContentEquals(expectedConciseRecords, actualConciseRecords)
    }

    @Test
    fun buildQueryForGetRecordsForAppPagingSourceTest() {
        fun testCase(
            limit: Int,
            offset: Int,
            sortType: RecordSortType,
            startEpochSeconds: Long,
            endEpochSeconds: Long,
            expectedQuery: String
        ) {
            val actualQuery =
                buildQueryForGetRecordsForAppPagingSource(limit, offset, sortType, startEpochSeconds, endEpochSeconds)

            assertEquals(expectedQuery, actualQuery)
        }

        // For RecordSortType.NEWEST
        testCase(
            limit = 1, offset = 10,
            sortType = RecordSortType.NEWEST,
            startEpochSeconds = 0L, endEpochSeconds = Long.MAX_VALUE,
            expectedQuery = "SELECT id, expression, meaning, score, dateTime FROM records ORDER BY dateTime DESC LIMIT 1 OFFSET 10"
        )

        testCase(
            limit = 1, offset = 10,
            sortType = RecordSortType.NEWEST,
            startEpochSeconds = 100L, endEpochSeconds = 123L,
            expectedQuery = "SELECT id, expression, meaning, score, dateTime FROM records WHERE dateTime BETWEEN 100 AND 123 ORDER BY dateTime DESC LIMIT 1 OFFSET 10"
        )

        testCase(
            limit = 1, offset = 10,
            sortType = RecordSortType.NEWEST,
            startEpochSeconds = 100L, endEpochSeconds = Long.MAX_VALUE,
            expectedQuery = "SELECT id, expression, meaning, score, dateTime FROM records WHERE dateTime >= 100 ORDER BY dateTime DESC LIMIT 1 OFFSET 10"
        )

        testCase(
            limit = 1, offset = 10,
            sortType = RecordSortType.NEWEST,
            startEpochSeconds = 0L, endEpochSeconds = 123L,
            expectedQuery = "SELECT id, expression, meaning, score, dateTime FROM records WHERE dateTime <= 123 ORDER BY dateTime DESC LIMIT 1 OFFSET 10"
        )

        // For RecordSortType.OLDEST
        testCase(
            limit = 1, offset = 10,
            sortType = RecordSortType.OLDEST,
            startEpochSeconds = 0L, endEpochSeconds = Long.MAX_VALUE,
            expectedQuery = "SELECT id, expression, meaning, score, dateTime FROM records ORDER BY dateTime ASC LIMIT 1 OFFSET 10"
        )

        testCase(
            limit = 1, offset = 10,
            sortType = RecordSortType.OLDEST,
            startEpochSeconds = 100L, endEpochSeconds = 123L,
            expectedQuery = "SELECT id, expression, meaning, score, dateTime FROM records WHERE dateTime BETWEEN 100 AND 123 ORDER BY dateTime ASC LIMIT 1 OFFSET 10"
        )

        testCase(
            limit = 1, offset = 10,
            sortType = RecordSortType.OLDEST,
            startEpochSeconds = 100L, endEpochSeconds = Long.MAX_VALUE,
            expectedQuery = "SELECT id, expression, meaning, score, dateTime FROM records WHERE dateTime >= 100 ORDER BY dateTime ASC LIMIT 1 OFFSET 10"
        )

        testCase(
            limit = 1, offset = 10,
            sortType = RecordSortType.OLDEST,
            startEpochSeconds = 0L, endEpochSeconds = 123L,
            expectedQuery = "SELECT id, expression, meaning, score, dateTime FROM records WHERE dateTime <= 123 ORDER BY dateTime ASC LIMIT 1 OFFSET 10"
        )

        // For RecordSortType.GREATEST_SCORE
        testCase(
            limit = 1, offset = 10,
            sortType = RecordSortType.GREATEST_SCORE,
            startEpochSeconds = 0L, endEpochSeconds = Long.MAX_VALUE,
            expectedQuery = "SELECT id, expression, meaning, score, dateTime FROM records ORDER BY score DESC LIMIT 1 OFFSET 10"
        )

        testCase(
            limit = 1, offset = 10,
            sortType = RecordSortType.GREATEST_SCORE,
            startEpochSeconds = 100L, endEpochSeconds = 123L,
            expectedQuery = "SELECT id, expression, meaning, score, dateTime FROM records WHERE dateTime BETWEEN 100 AND 123 ORDER BY score DESC LIMIT 1 OFFSET 10"
        )

        testCase(
            limit = 1, offset = 10,
            sortType = RecordSortType.GREATEST_SCORE,
            startEpochSeconds = 100L, endEpochSeconds = Long.MAX_VALUE,
            expectedQuery = "SELECT id, expression, meaning, score, dateTime FROM records WHERE dateTime >= 100 ORDER BY score DESC LIMIT 1 OFFSET 10"
        )

        testCase(
            limit = 1, offset = 10,
            sortType = RecordSortType.GREATEST_SCORE,
            startEpochSeconds = 0L, endEpochSeconds = 123L,
            expectedQuery = "SELECT id, expression, meaning, score, dateTime FROM records WHERE dateTime <= 123 ORDER BY score DESC LIMIT 1 OFFSET 10"
        )


        // For RecordSortType.LEAST_SCORE
        testCase(
            limit = 1, offset = 10,
            sortType = RecordSortType.LEAST_SCORE,
            startEpochSeconds = 0L, endEpochSeconds = Long.MAX_VALUE,
            expectedQuery = "SELECT id, expression, meaning, score, dateTime FROM records ORDER BY score ASC LIMIT 1 OFFSET 10"
        )

        testCase(
            limit = 1, offset = 10,
            sortType = RecordSortType.LEAST_SCORE,
            startEpochSeconds = 100L, endEpochSeconds = 123L,
            expectedQuery = "SELECT id, expression, meaning, score, dateTime FROM records WHERE dateTime BETWEEN 100 AND 123 ORDER BY score ASC LIMIT 1 OFFSET 10"
        )

        testCase(
            limit = 1, offset = 10,
            sortType = RecordSortType.LEAST_SCORE,
            startEpochSeconds = 100L, endEpochSeconds = Long.MAX_VALUE,
            expectedQuery = "SELECT id, expression, meaning, score, dateTime FROM records WHERE dateTime >= 100 ORDER BY score ASC LIMIT 1 OFFSET 10"
        )

        testCase(
            limit = 1, offset = 10,
            sortType = RecordSortType.LEAST_SCORE,
            startEpochSeconds = 0L, endEpochSeconds = 123L,
            expectedQuery = "SELECT id, expression, meaning, score, dateTime FROM records WHERE dateTime <= 123 ORDER BY score ASC LIMIT 1 OFFSET 10"
        )

        // For RecordSortType.ALPHABETIC_BY_EXPRESSION
        // The queries should not have a ORDER BY as the result is sorted on managed side.
        testCase(
            limit = 1, offset = 10,
            sortType = RecordSortType.ALPHABETIC_BY_EXPRESSION,
            startEpochSeconds = 0L, endEpochSeconds = Long.MAX_VALUE,
            expectedQuery = "SELECT id, expression, meaning, score, dateTime FROM records LIMIT 1 OFFSET 10"
        )

        testCase(
            limit = 1, offset = 10,
            sortType = RecordSortType.ALPHABETIC_BY_EXPRESSION,
            startEpochSeconds = 100L, endEpochSeconds = 123L,
            expectedQuery = "SELECT id, expression, meaning, score, dateTime FROM records WHERE dateTime BETWEEN 100 AND 123 LIMIT 1 OFFSET 10"
        )

        testCase(
            limit = 1, offset = 10,
            sortType = RecordSortType.ALPHABETIC_BY_EXPRESSION,
            startEpochSeconds = 100L, endEpochSeconds = Long.MAX_VALUE,
            expectedQuery = "SELECT id, expression, meaning, score, dateTime FROM records WHERE dateTime >= 100 LIMIT 1 OFFSET 10"
        )

        testCase(
            limit = 1, offset = 10,
            sortType = RecordSortType.ALPHABETIC_BY_EXPRESSION,
            startEpochSeconds = 0L, endEpochSeconds = 123L,
            expectedQuery = "SELECT id, expression, meaning, score, dateTime FROM records WHERE dateTime <= 123 LIMIT 1 OFFSET 10"
        )

        // For RecordSortType.ALPHABETIC_BY_EXPRESSION_INVERSE
        // The queries should not have a ORDER BY as the result is sorted on managed side.
        testCase(
            limit = 1, offset = 10,
            sortType = RecordSortType.ALPHABETIC_BY_EXPRESSION_INVERSE,
            startEpochSeconds = 0L, endEpochSeconds = Long.MAX_VALUE,
            expectedQuery = "SELECT id, expression, meaning, score, dateTime FROM records LIMIT 1 OFFSET 10"
        )

        testCase(
            limit = 1, offset = 10,
            sortType = RecordSortType.ALPHABETIC_BY_EXPRESSION_INVERSE,
            startEpochSeconds = 100L, endEpochSeconds = 123L,
            expectedQuery = "SELECT id, expression, meaning, score, dateTime FROM records WHERE dateTime BETWEEN 100 AND 123 LIMIT 1 OFFSET 10"
        )

        testCase(
            limit = 1, offset = 10,
            sortType = RecordSortType.ALPHABETIC_BY_EXPRESSION_INVERSE,
            startEpochSeconds = 100L, endEpochSeconds = Long.MAX_VALUE,
            expectedQuery = "SELECT id, expression, meaning, score, dateTime FROM records WHERE dateTime >= 100 LIMIT 1 OFFSET 10"
        )

        testCase(
            limit = 1, offset = 10,
            sortType = RecordSortType.ALPHABETIC_BY_EXPRESSION_INVERSE,
            startEpochSeconds = 0L, endEpochSeconds = 123L,
            expectedQuery = "SELECT id, expression, meaning, score, dateTime FROM records WHERE dateTime <= 123 LIMIT 1 OFFSET 10"
        )
    }

    @Suppress("UNCHECKED_CAST")
    private fun getConciseRecordsWithBadgesForAppPagingSourceTestHelper(sortType: RecordSortType) {
        val db = createEmptyDatabase()

        val records = Array(20) { i ->
            Record(
                expression = "E$i",
                meaning = "CMeaning$i",
                additionalNotes = "A$i",
                score = i * 10,
                epochSeconds = i * 100L
            )
        }

        val badges = arrayOf(
            RecordBadgeInfo(name = "Badge1", outlineColor = 1)
        )

        val relations = Array(20) { i ->
            RecordToBadgeRelation(recordId = i + 1, badgeId = 1)
        }

        db.insertRecordsAndBadges(records, badges, relations)

        val actualConciseRecords = db.getConciseRecordsWithBadgesForAppPagingSource(
            limit = 10, offset = 5,
            sortType,
            startEpochSeconds = 0L, endEpochSeconds = Long.MAX_VALUE
        )
        val expectedConciseRecords = Array(10) { i ->
            val indexWithOffset = i + 5

            ConciseRecordWithBadges(
                id = indexWithOffset + 1,
                expression = "E${indexWithOffset}",
                meaning = "CMeaning${indexWithOffset}",
                score = indexWithOffset * 10,
                epochSeconds = indexWithOffset * 100L,
                badges = arrayOf(
                    RecordBadgeInfo(id = 1, name = "Badge1", outlineColor = 1)
                )
            )
        }.sortedArrayWith(sortType.getComparatorForConciseRecordWithBadges())

        assertContentEquals(expectedConciseRecords as Array<ConciseRecordWithBadges>, actualConciseRecords)
    }

    @Test
    fun getConciseRecordsWithBadgesForAppPagingSource_newestTest() {
        getConciseRecordsWithBadgesForAppPagingSourceTestHelper(RecordSortType.NEWEST)
    }

    @Test
    fun getConciseRecordsWithBadgesForAppPagingSource_oldestTest() {
        getConciseRecordsWithBadgesForAppPagingSourceTestHelper(RecordSortType.OLDEST)
    }

    @Test
    fun getConciseRecordsWithBadgesForAppPagingSource_greatestScoreTest() {
        getConciseRecordsWithBadgesForAppPagingSourceTestHelper(RecordSortType.GREATEST_SCORE)
    }

    @Test
    fun getConciseRecordsWithBadgesForAppPagingSource_leastScoreTest() {
        getConciseRecordsWithBadgesForAppPagingSourceTestHelper(RecordSortType.LEAST_SCORE)
    }

    @Test
    fun getConciseRecordsWithBadgesForAppPagingSource_alphabeticByExpressionTest() {
        getConciseRecordsWithBadgesForAppPagingSourceTestHelper(RecordSortType.ALPHABETIC_BY_EXPRESSION)
    }

    @Test
    fun getConciseRecordsWithBadgesForAppPagingSource_alphabeticByExpressionInverseTest() {
        getConciseRecordsWithBadgesForAppPagingSourceTestHelper(RecordSortType.ALPHABETIC_BY_EXPRESSION_INVERSE)
    }

    @Test
    fun buildQueryForCountStatementTest() {
        fun testCase(startEpochSeconds: Long, endEpochSeconds: Long, expectedSql: String) {
            val actualSql = buildQueryForCountStatement(startEpochSeconds, endEpochSeconds)

            assertEquals(expectedSql, actualSql)
        }

        testCase(
            startEpochSeconds = 0L,
            endEpochSeconds = Long.MAX_VALUE,
            expectedSql = "SELECT COUNT(*) FROM records"
        )

        testCase(
            startEpochSeconds = 100L,
            endEpochSeconds = 123L,
            expectedSql = "SELECT COUNT(*) FROM records WHERE dateTime BETWEEN 100 AND 123"
        )

        testCase(
            startEpochSeconds = 100L,
            endEpochSeconds = Long.MAX_VALUE,
            expectedSql = "SELECT COUNT(*) FROM records WHERE dateTime >= 100"
        )

        testCase(
            startEpochSeconds = 0L,
            endEpochSeconds = 123L,
            expectedSql = "SELECT COUNT(*) FROM records WHERE dateTime <= 123"
        )
    }

    @Test
    fun compileCountStatementTest() {
        fun testCase(recordCount: Int) {
            val db = createEmptyDatabase()
            val recordDao = db.recordDao()
            val countStatement = db.compileCountStatement()

            runBlocking {
                repeat(recordCount) { i ->
                    recordDao.insert(
                        Record(
                            id = 0,
                            expression = "Ex$i",
                            meaning = "C$i",
                            additionalNotes = "A$i",
                            score = i,
                            epochSeconds = i * 10L
                        )
                    )
                }
            }

            val actualCount = countStatement.simpleQueryForLong().toInt()
            assertEquals(recordCount, actualCount)
        }

        testCase(recordCount = 0)
        testCase(recordCount = 1)
        testCase(recordCount = 100)
    }

    @Test
    fun compileTimeRangedCountStatementTest() {
        fun testCase(startEpochSeconds: Long, endEpochSeconds: Long, expectedCount: Int) {
            val db = createEmptyDatabase()
            val recordDao = db.recordDao()
            val countStatement = db.compileCountStatement(startEpochSeconds, endEpochSeconds)

            runBlocking {
                repeat(10) { i ->
                    recordDao.insert(
                        Record(
                            id = 0,
                            expression = "Ex$i",
                            meaning = "C$i",
                            additionalNotes = "A$i",
                            score = i,
                            epochSeconds = i * 10L
                        )
                    )
                }
            }

            val actualCount = countStatement.simpleQueryForLong().toInt()

            assertEquals(expectedCount, actualCount)
        }

        testCase(startEpochSeconds = 0, endEpochSeconds = Long.MAX_VALUE, expectedCount = 10)
        testCase(startEpochSeconds = 10, endEpochSeconds = 70, expectedCount = 7)
        testCase(startEpochSeconds = 110, endEpochSeconds = 200, expectedCount = 0)
    }
}