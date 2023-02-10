package io.github.pelmenstar1.digiDict.data

import androidx.sqlite.db.SupportSQLiteProgram
import androidx.sqlite.db.SupportSQLiteQuery
import androidx.sqlite.db.SupportSQLiteStatement
import io.github.pelmenstar1.digiDict.common.ProgressReporter
import io.github.pelmenstar1.digiDict.common.android.queryArrayWithProgressReporter
import io.github.pelmenstar1.digiDict.common.unsafeNewArray

private const val CONCISE_RECORD_VALUES = "id, expression, meaning, score, dateTime"

fun AppDatabase.compileInsertRecordStatement(): SupportSQLiteStatement {
    return compileStatement("INSERT OR REPLACE INTO records (expression, meaning, additionalNotes, score, dateTime) VALUES (?,?,?,?,?)")
}

fun SupportSQLiteStatement.bindRecordToInsertStatement(record: Record) {
    bindString(1, record.expression)
    bindString(2, record.meaning)
    bindString(3, record.additionalNotes)
    bindLong(4, record.score.toLong())
    bindLong(5, record.epochSeconds)
}

fun AppDatabase.compileInsertOrReplaceRecordBadgeStatement(): SupportSQLiteStatement {
    return compileStatement("INSERT OR REPLACE INTO record_badges (name, outlineColor) VALUES (?,?)")
}

fun AppDatabase.compileInsertRecordBadgeStatement(): SupportSQLiteStatement {
    return compileStatement("INSERT INTO record_badges (name, outlineColor) VALUES (?,?)")
}

fun SupportSQLiteStatement.bindRecordBadgeToInsertStatement(badge: RecordBadgeInfo) {
    bindString(1, badge.name)
    bindLong(2, badge.outlineColor.toLong())
}

fun AppDatabase.compileInsertRecordToBadgeRelation(): SupportSQLiteStatement {
    return compileStatement("INSERT INTO record_to_badge_relations (recordId, badgeId) VALUES (?, ?)")
}

fun SupportSQLiteStatement.bindRecordToBadgeInsertStatement(recordId: Int, badgeId: Int) {
    bindLong(1, recordId.toLong())
    bindLong(2, badgeId.toLong())
}

fun SupportSQLiteStatement.bindRecordToBadgeInsertStatement(relation: RecordToBadgeRelation) {
    bindRecordToBadgeInsertStatement(relation.recordId, relation.badgeId)
}

fun AppDatabase.compileCountStatement(): SupportSQLiteStatement {
    return compileStatement("SELECT COUNT(*) FROM records")
}

fun AppDatabase.compileTimeRangedCountStatement(): SupportSQLiteStatement {
    return compileStatement("SELECT COUNT(*) FROM records WHERE dateTime BETWEEN ? AND ?")
}

fun SupportSQLiteStatement.bindToTimeRangedCountStatement(startEpochSeconds: Long, endEpochSeconds: Long) {
    bindLong(1, startEpochSeconds)
    bindLong(2, endEpochSeconds)
}

fun AppDatabase.getAllRecordsOrderByIdAsc(progressReporter: ProgressReporter?): Array<Record> {
    return queryArrayWithProgressReporter(
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

fun AppDatabase.getAllRecordBadgesOrderByIdAsc(progressReporter: ProgressReporter?): Array<RecordBadgeInfo> {
    return queryArrayWithProgressReporter(
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

fun AppDatabase.getAllRecordToBadgeRelations(progressReporter: ProgressReporter?): Array<RecordToBadgeRelation> {
    return queryArrayWithProgressReporter(
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

class GetBadgesByRecordIdQuery : SupportSQLiteQuery {
    var recordId: Int = 0

    override fun getSql() = """
        SELECT rb.id, rb.name, rb.outlineColor
        FROM record_badges AS rb 
        WHERE rb.id IN (SELECT rbr.badgeId FROM record_to_badge_relations AS rbr WHERE rbr.recordId=?) 
        """

    override fun bindTo(statement: SupportSQLiteProgram) {
        statement.bindLong(1, recordId.toLong())
    }

    override fun getArgCount() = 1
}

fun AppDatabase.getBadgesByRecordId(getQuery: GetBadgesByRecordIdQuery, recordId: Int): Array<RecordBadgeInfo> {
    getQuery.recordId = recordId

    return query(getQuery).use { c ->
        val count = c.count
        val result = unsafeNewArray<RecordBadgeInfo>(count)

        for (i in 0 until count) {
            c.moveToPosition(i)

            val id = c.getInt(0)
            val name = c.getString(1)
            val outlineColor = c.getInt(2)

            result[i] = RecordBadgeInfo(id, name, outlineColor)
        }

        result
    }
}

fun AppDatabase.getAllConciseRecordsWithBadges(progressReporter: ProgressReporter?): Array<ConciseRecordWithBadges> {
    return queryConciseRecordsWithBadges("SELECT $CONCISE_RECORD_VALUES FROM records", progressReporter)
}

private val sqlRecordSortTypes = arrayOf(
    "${RecordTable.epochSeconds} DESC", // RecordSortType.NEWEST
    "${RecordTable.epochSeconds} ASC", // RecordSortType.OLDEST
    "${RecordTable.score} DESC", // RecordSortType.GREATEST_SCORE
    "${RecordTable.score} ASC", // RecordSortType.LEAST_SCORE
    // RecordSortType.ALPHABETIC_BY_EXPRESSION and RecordSortType.ALPHABETIC_BY_EXPRESSION_INVERSE have different handling
    // and this array is not used in that case. See below
)

internal fun buildQueryForGetRecordsForAppPagingSource(
    limit: Int, offset: Int,
    sortType: RecordSortType,
    startEpochSeconds: Long, endEpochSeconds: Long
): String {
    return buildString(128) {
        append("SELECT $CONCISE_RECORD_VALUES FROM records")

        if (startEpochSeconds > 0L && endEpochSeconds < Long.MAX_VALUE) {
            append(" WHERE dateTime BETWEEN ")
            append(startEpochSeconds)
            append(" AND ")
            append(endEpochSeconds)
        }

        // SQLite 'ORDER BY' sorts strings differently than RecordSortType's comparator does. To unify behaviour
        // we sort the array manually and don't need additional ORDER BY
        //
        // RecordSortType.ALPHABETIC_BY_EXPRESSION.ordinal = 4
        // RecordSortType.ALPHABETIC_BY_EXPRESSION_INVERSE.ordinal = 5
        val sortTypeOrdinal = sortType.ordinal

        if (sortTypeOrdinal != 4 && sortTypeOrdinal != 5) {
            append(" ORDER BY ")
            append(sqlRecordSortTypes[sortTypeOrdinal])
        }

        append(" LIMIT ")
        append(limit)
        append(" OFFSET ")
        append(offset)
    }
}

fun AppDatabase.getConciseRecordsWithBadgesForAppPagingSource(
    limit: Int, offset: Int,
    sortType: RecordSortType,
    startEpochSeconds: Long, endEpochSeconds: Long
): Array<ConciseRecordWithBadges> {
    val result = queryConciseRecordsWithBadges(
        buildQueryForGetRecordsForAppPagingSource(limit, offset, sortType, startEpochSeconds, endEpochSeconds),
        progressReporter = null
    )

    // SQLite 'ORDER BY' sorts strings differently than RecordSortType's comparator does. To unify behaviour
    // we sort the array manually.
    if (sortType == RecordSortType.ALPHABETIC_BY_EXPRESSION || sortType == RecordSortType.ALPHABETIC_BY_EXPRESSION_INVERSE) {
        result.sortWith(sortType.getComparatorForConciseRecordWithBadges())
    }

    return result
}

private fun AppDatabase.queryConciseRecordsWithBadges(
    sql: String,
    progressReporter: ProgressReporter?
): Array<ConciseRecordWithBadges> {
    val getBadgesByRecordIdQuery = GetBadgesByRecordIdQuery()

    return queryArrayWithProgressReporter(sql, null, progressReporter) { c ->
        val id = c.getInt(0)
        val expr = c.getString(1)
        val meaning = c.getString(2)
        val score = c.getInt(3)
        val epochSeconds = c.getLong(4)

        val badges = getBadgesByRecordId(getBadgesByRecordIdQuery, id)

        ConciseRecordWithBadges(id, expr, meaning, score, epochSeconds, badges)
    }
}