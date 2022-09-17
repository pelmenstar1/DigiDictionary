package io.github.pelmenstar1.digiDict.data

import androidx.sqlite.db.SupportSQLiteStatement
import io.github.pelmenstar1.digiDict.common.ProgressReporter
import io.github.pelmenstar1.digiDict.common.queryArrayWithProgressReporter

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