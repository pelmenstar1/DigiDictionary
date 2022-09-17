package io.github.pelmenstar1.digiDict.data

import androidx.sqlite.db.SupportSQLiteProgram
import androidx.sqlite.db.SupportSQLiteQuery
import androidx.sqlite.db.SupportSQLiteStatement
import io.github.pelmenstar1.digiDict.common.ProgressReporter
import io.github.pelmenstar1.digiDict.common.queryArrayWithProgressReporter
import io.github.pelmenstar1.digiDict.common.unsafeNewArray

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
    val getBadgesByRecordIdQuery = GetBadgesByRecordIdQuery()

    return queryArrayWithProgressReporter(
        "SELECT id, expression, meaning, score FROM records",
        null,
        progressReporter
    ) { c ->
        val id = c.getInt(0)
        val expr = c.getString(1)
        val meaning = c.getString(2)
        val score = c.getInt(3)

        val badges = getBadgesByRecordId(getBadgesByRecordIdQuery, id)

        ConciseRecordWithBadges(id, expr, meaning, score, badges)
    }
}

