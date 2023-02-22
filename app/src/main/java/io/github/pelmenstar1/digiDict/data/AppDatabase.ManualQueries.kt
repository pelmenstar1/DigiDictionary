package io.github.pelmenstar1.digiDict.data

import androidx.sqlite.db.SupportSQLiteStatement
import io.github.pelmenstar1.digiDict.common.ProgressReporter
import io.github.pelmenstar1.digiDict.common.android.queryArrayWithProgressReporter
import io.github.pelmenstar1.digiDict.common.unsafeNewArray

private const val CONCISE_RECORD_VALUES = "id, expression, meaning, score, dateTime"

fun AppDatabase.compileInsertRecordStatement(): SupportSQLiteStatement {
    return compileStatement("INSERT OR REPLACE INTO records (expression, meaning, additionalNotes, score, dateTime) VALUES (?,?,?,?,?)")
}

fun SupportSQLiteStatement.bindRecordToInsertStatement(record: Record) {
    bindRecordToInsertStatement(
        record.expression,
        record.meaning,
        record.additionalNotes,
        record.score,
        record.epochSeconds
    )
}

fun SupportSQLiteStatement.bindRecordToInsertStatement(
    expression: String,
    meaning: String,
    additionalNotes: String,
    score: Int,
    epochSeconds: Long
) {
    bindString(1, expression)
    bindString(2, meaning)
    bindString(3, additionalNotes)
    bindLong(4, score.toLong())
    bindLong(5, epochSeconds)
}

fun AppDatabase.compileInsertOrReplaceRecordBadgeStatement(): SupportSQLiteStatement {
    return compileStatement("INSERT OR REPLACE INTO record_badges (name, outlineColor) VALUES (?,?)")
}

fun AppDatabase.compileInsertRecordBadgeStatement(): SupportSQLiteStatement {
    return compileStatement("INSERT INTO record_badges (name, outlineColor) VALUES (?,?)")
}

fun SupportSQLiteStatement.bindRecordBadgeToInsertStatement(badge: RecordBadgeInfo) {
    bindRecordBadgeToInsertStatement(badge.name, badge.outlineColor)
}

fun SupportSQLiteStatement.bindRecordBadgeToInsertStatement(name: String, outlineColor: Int) {
    bindString(1, name)
    bindLong(2, outlineColor.toLong())
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

/**
 * Builds an SQL query for the statement that counts the records within some time range
 * The expected queries are:
 * - If [startEpochSeconds] = 0 and [endEpochSeconds] = [Long.MAX_VALUE], query is `SELECT COUNT(*) FROM records`
 * - If [startEpochSeconds] != 0 and [endEpochSeconds] != [Long.MAX_VALUE], query is `SELECT COUNT(*) FROM records BETWEEN start AND end`
 * - If [startEpochSeconds] > 0, query is `SELECT COUNT(*) FROM records dateTime >= start`
 * - If [endEpochSeconds] < [Long.MAX_VALUE], query = `SELECT COUNT(*) FROM records dateTime <= end`
 */
internal fun buildQueryForCountStatement(startEpochSeconds: Long, endEpochSeconds: Long): String {
    if (startEpochSeconds == 0L && endEpochSeconds == Long.MAX_VALUE) {
        return "SELECT COUNT(*) FROM records"
    }

    return buildString(64) {
        append("SELECT COUNT(*) FROM records WHERE dateTime ")

        if (startEpochSeconds > 0 && endEpochSeconds < Long.MAX_VALUE) {
            append("BETWEEN ")
            append(startEpochSeconds)
            append(" AND ")
            append(endEpochSeconds)
        } else if (startEpochSeconds > 0) {
            append(">= ")
            append(startEpochSeconds)
        } else {
            append("<= ")
            append(endEpochSeconds)
        }
    }
}

fun AppDatabase.compileCountStatement(
    startEpochSeconds: Long = 0L,
    endEpochSeconds: Long = Long.MAX_VALUE
): SupportSQLiteStatement {
    return compileStatement(buildQueryForCountStatement(startEpochSeconds, endEpochSeconds))
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

fun AppDatabase.getAllConciseRecordsWithBadges(
    allSortedPackedRelations: PackedRecordToBadgeRelationArray,
    progressReporter: ProgressReporter?
): Array<ConciseRecordWithBadges> {
    return getConciseRecordsWithBadges(
        "SELECT $CONCISE_RECORD_VALUES FROM records",
        allSortedPackedRelations,
        progressReporter
    )
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
        } else if (startEpochSeconds > 0L) {
            append(" WHERE dateTime >= ")
            append(startEpochSeconds)
        } else if (endEpochSeconds < Long.MAX_VALUE) {
            append(" WHERE dateTime <= ")
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
    startEpochSeconds: Long, endEpochSeconds: Long,
    allSortedPackedRelations: PackedRecordToBadgeRelationArray
): Array<ConciseRecordWithBadges> {
    val result = getConciseRecordsWithBadges(
        buildQueryForGetRecordsForAppPagingSource(limit, offset, sortType, startEpochSeconds, endEpochSeconds),
        allSortedPackedRelations,
        progressReporter = null
    )

    // SQLite 'ORDER BY' sorts strings differently than RecordSortType's comparator does. To unify behaviour
    // we sort the array manually.
    if (sortType == RecordSortType.ALPHABETIC_BY_EXPRESSION || sortType == RecordSortType.ALPHABETIC_BY_EXPRESSION_INVERSE) {
        result.sortWith(sortType.getComparatorForConciseRecordWithBadges())
    }

    return result
}

private fun findRecordIdStartInBadgeRelations(
    index: Int,
    recordId: Int,
    sortedRelations: PackedRecordToBadgeRelationArray,
): Int {
    var i = index

    while (i > 0) {
        val prevRecordId = sortedRelations[--i].recordId

        if (prevRecordId != recordId) {
            return i + 1
        }
    }

    return i
}

private fun findRecordIdEndInBadgeRelations(
    index: Int,
    recordId: Int,
    sortedRelations: PackedRecordToBadgeRelationArray
): Int {
    val size = sortedRelations.size

    for (i in (index + 1) until size) {
        if (sortedRelations[i].recordId != recordId) {
            return i
        }
    }

    return size
}

private fun buildGetBadgesByRecordIdQuery(relations: PackedRecordToBadgeRelationArray, start: Int, end: Int): String {
    return buildString(64) {
        append("SELECT * FROM record_badges WHERE id IN (")

        for (i in start until end) {
            append(relations[i].badgeId)

            if (i < end - 1) {
                append(',')
            }
        }
        append(')')
    }
}

/**
 * Makes a query to DB in order to get all badges assigned to specified [recordId].
 * [relations] is an array of all record-to-badge relations sorted by record id.
 */
fun AppDatabase.getBadgesByRecordId(
    recordId: Int,
    relations: PackedRecordToBadgeRelationArray,
): Array<RecordBadgeInfo> {
    val index = relations.binarySearchRecordId(recordId)
    if (index < 0) {
        return RecordBadgeInfo.EMPTY_ARRAY
    }

    val start = findRecordIdStartInBadgeRelations(index, recordId, relations)
    val end = findRecordIdEndInBadgeRelations(index, recordId, relations)
    val sql = buildGetBadgesByRecordIdQuery(relations, start, end)

    return query(sql, null).use { c ->
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

/**
 * Make a query, with specified [sql], to DB in order to get concise records.
 * A progress can be reported to [progressReporter] if supplied.
 * [allSortedPackedRelations] is an array of all record-to-badge relations sorted by record id.
 */
private fun AppDatabase.getConciseRecordsWithBadges(
    sql: String,
    allSortedPackedRelations: PackedRecordToBadgeRelationArray,
    progressReporter: ProgressReporter?
): Array<ConciseRecordWithBadges> {
    return queryArrayWithProgressReporter(sql, null, progressReporter) { c ->
        val id = c.getInt(0)
        val expr = c.getString(1)
        val meaning = c.getString(2)
        val score = c.getInt(3)
        val epochSeconds = c.getLong(4)

        val badges = getBadgesByRecordId(id, allSortedPackedRelations)

        ConciseRecordWithBadges(id, expr, meaning, score, epochSeconds, badges)
    }
}

/**
 * Makes a query to DB in order to get all record to badge relations packed into [Long] and sorted by record id.
 */
fun AppDatabase.getAllSortedPackedRecordToBadgeRelations(): PackedRecordToBadgeRelationArray {
    return query("SELECT recordId, badgeId FROM record_to_badge_relations ORDER BY recordId ASC", null).use { cursor ->
        val count = cursor.count
        val result = PackedRecordToBadgeRelationArray(count)

        for (i in 0 until count) {
            cursor.moveToPosition(i)

            val recordId = cursor.getInt(0)
            val badgeId = cursor.getInt(1)

            result[i] = PackedRecordToBadgeRelation(recordId, badgeId)
        }

        result
    }
}