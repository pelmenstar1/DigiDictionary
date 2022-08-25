package io.github.pelmenstar1.digiDict.data

import android.database.CharArrayBuffer
import android.database.Cursor
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import io.github.pelmenstar1.digiDict.common.equalsPattern
import io.github.pelmenstar1.digiDict.common.serialization.*

@Entity(
    tableName = "records",
    indices = [Index(RecordTable.expression, unique = true)]
)
open class Record(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = RecordTable.id)
    final override val id: Int,
    @ColumnInfo(name = RecordTable.expression) val expression: String,
    @ColumnInfo(name = RecordTable.meaning) val meaning: String,
    @ColumnInfo(name = RecordTable.additionalNotes) val additionalNotes: String,
    @ColumnInfo(name = RecordTable.score) val score: Int,
    // in UTC
    @ColumnInfo(name = RecordTable.epochSeconds) val epochSeconds: Long,
) : EntityWithPrimaryKeyId {
    init {
        require(id >= 0) { "Id can't be negative" }
        require(epochSeconds >= 0) { "Epoch seconds can't be negative" }
    }

    override fun equals(other: Any?) = equalsPattern(other) { o ->
        return id == o.id && equalsNoId(other)
    }

    override fun equalsNoId(other: Any?) = equalsPattern(other) { o ->
        return expression == o.expression &&
                meaning == o.meaning &&
                additionalNotes == o.additionalNotes &&
                score == o.score &&
                epochSeconds == o.epochSeconds
    }

    override fun hashCode(): Int {
        var result = id
        result = result * 31 + expression.hashCode()
        result = result * 31 + meaning.hashCode()
        result = result * 31 + additionalNotes.hashCode()
        result = result * 31 + score
        result = result * 31 + epochSeconds.hashCode()

        return result
    }

    override fun toString(): String {
        return "Record(id=$id, expression='$expression', meaning='$meaning', additionalNotes=${additionalNotes}, score=$score, epochSeconds=$epochSeconds)"
    }

    companion object {
        val EXPRESSION_COMPARATOR = Comparator<Record> { a, b ->
            a.expression.compareTo(b.expression)
        }

        val NO_ID_SERIALIZER_RESOLVER = MultiVersionBinarySerializerResolver<Record> {
            forVersion<Record>(
                version = 1,
                getByteSize = { value ->
                    stringUtf16(value.expression) +
                            stringUtf16(value.meaning) +
                            stringUtf16(value.additionalNotes) +
                            int32 /* score */ +
                            int64 /* epochSeconds */
                },
                write = { value ->
                    stringUtf16(value.expression)
                    stringUtf16(value.meaning)
                    stringUtf16(value.additionalNotes)
                    int32(value.score)
                    int64(value.epochSeconds)
                },
                read = {
                    val expression = stringUtf16()
                    val rawMeaning = stringUtf16()
                    val additionalNotes = stringUtf16()
                    val score = int32()
                    val epochSeconds = int64()

                    if (epochSeconds < 0) {
                        throw ValidationException("Epoch seconds can't be negative")
                    }

                    Record(
                        id = 0,
                        expression, rawMeaning, additionalNotes,
                        score,
                        epochSeconds
                    )
                }
            )
        }
    }
}

open class ConciseRecord(
    override val id: Int,
    val expression: String,
    val meaning: String,
    val score: Int
) : EntityWithPrimaryKeyId {
    override fun equals(other: Any?) = equalsPattern(other) { o ->
        id == o.id && equalsNoId(other)
    }

    override fun equalsNoId(other: Any?) = equalsPattern(other) { o ->
        expression == o.expression && meaning == o.meaning && score == o.score
    }

    override fun hashCode(): Int {
        var result = id
        result = 31 * result + expression.hashCode()
        result = 31 * result + meaning.hashCode()
        result = 31 * result + score

        return result
    }

    override fun toString(): String {
        return "ConciseRecord(id=$id, expression=$expression, meaning=$meaning, score=$score)"
    }

    companion object {
        fun Record.toConciseRecord() = ConciseRecord(id, expression, meaning, score)
    }
}

open class ConciseRecordWithBadges(
    id: Int,
    expression: String,
    meaning: String,
    score: Int,
    val badges: Array<RecordBadgeInfo>
) : ConciseRecord(id, expression, meaning, score) {
    override fun equals(other: Any?) = equalsPattern(other) { o ->
        id == o.id && equalsNoId(o)
    }

    override fun equalsNoId(other: Any?) = equalsPattern(other) { o ->
        return expression == o.expression && meaning == o.meaning && score == o.score && badges.contentEquals(o.badges)
    }

    override fun hashCode(): Int {
        var result = super.hashCode()
        result = result * 31 + badges.contentHashCode()

        return result
    }

    override fun toString(): String {
        return "ConciseRecordWithBadges(id=$id, expression=$expression, meaning=$meaning, score=$score, badges=${badges.contentToString()})"
    }

    companion object {
        fun create(record: ConciseRecord, badges: Array<RecordBadgeInfo>) = ConciseRecordWithBadges(
            record.id, record.expression, record.meaning, record.score, badges
        )
    }
}

@Suppress("EqualsOrHashCode") // ConciseRecord defines equals()
class ConciseRecordWithSearchInfo(
    id: Int,
    expression: String,
    meaning: String,
    score: Int,
    val keywords: String?
) : ConciseRecord(id, expression, meaning, score) {
    override fun equals(other: Any?) = equalsPattern(other) { o ->
        id == o.id && equalsNoId(o)
    }

    override fun equalsNoId(other: Any?) = equalsPattern(other) { o ->
        expression == o.expression && meaning == o.meaning && score == o.score && keywords == o.keywords
    }

    override fun hashCode(): Int {
        var result = super.hashCode()
        result = result * 31 + keywords.hashCode()

        return result
    }

    override fun toString(): String {
        return "ConciseRecordWithSearchInfoAndBadges(id=$id, expression=$expression, meaning=$meaning, score=$score, keywords=$keywords)"
    }
}

class ConciseRecordWithSearchInfoAndBadges(
    id: Int,
    expression: String,
    meaning: String,
    score: Int,
    badges: Array<RecordBadgeInfo>,
    val keywords: String?
) : ConciseRecordWithBadges(id, expression, meaning, score, badges) {
    override fun equals(other: Any?) = equalsPattern(other) { o ->
        id == o.id && equalsNoId(o)
    }

    override fun equalsNoId(other: Any?) = equalsPattern(other) { o ->
        expression == o.expression &&
                meaning == o.meaning &&
                score == o.score &&
                badges.contentEquals(o.badges) &&
                keywords == o.keywords
    }

    override fun hashCode(): Int {
        var result = super.hashCode()
        result = result * 31 + keywords.hashCode()

        return result
    }

    override fun toString(): String {
        return "ConciseRecordWithSearchInfoAndBadges(id=$id, expression=$expression, meaning=$meaning, score=$score, badges=${badges.contentToString()}, keywords=$keywords)"
    }

    companion object {
        fun create(record: ConciseRecordWithSearchInfo, badges: Array<RecordBadgeInfo>) =
            ConciseRecordWithSearchInfoAndBadges(
                record.id, record.expression, record.meaning, record.score, badges, record.keywords
            )
    }
}

@Suppress("EqualsOrHashCode") // equals() is declared in Record
open class RecordWithBadges(
    id: Int,
    expression: String,
    meaning: String,
    additionalNotes: String,
    score: Int,
    epochSeconds: Long,
    val badges: Array<RecordBadgeInfo>
) : Record(id, expression, meaning, additionalNotes, score, epochSeconds) {
    override fun equals(other: Any?) = equalsPattern(other) { o ->
        id == o.id && equalsNoId(other)
    }

    override fun equalsNoId(other: Any?) = equalsPattern(other) { o ->
        expression == o.expression &&
                meaning == o.meaning &&
                additionalNotes == o.additionalNotes &&
                score == o.score &&
                epochSeconds == o.epochSeconds &&
                badges.contentEquals(o.badges)
    }

    override fun hashCode(): Int {
        var result = super.hashCode()
        result = result * 31 + badges.contentHashCode()

        return result
    }

    override fun toString(): String {
        return "Record(id=$id, expression='$expression', meaning='$meaning', additionalNotes='$additionalNotes', score=$score, epochSeconds=$epochSeconds, badges=${badges.contentToString()}"
    }

    companion object {
        fun create(record: Record, badges: Array<RecordBadgeInfo>) = RecordWithBadges(
            record.id, record.expression, record.meaning, record.additionalNotes, record.score, record.epochSeconds,
            badges
        )
    }
}

object RecordTable {
    const val name = "records"

    const val id = "id"
    const val expression = "expression"
    const val meaning = "meaning"
    const val additionalNotes = "additionalNotes"
    const val score = "score"
    const val epochSeconds = "dateTime"
}

fun Cursor.asRecordSerializableIterableNoId(): SerializableIterable {
    val size = count
    val cursor = this

    return object : SerializableIterable {
        override val size: Int
            get() = size

        private val exprIndex = columnIndex { expression }
        private val meaningIndex = columnIndex { meaning }
        private val additionalNotesIndex = columnIndex { additionalNotes }
        private val scoreIndex = columnIndex { score }
        private val epochSecondsIndex = columnIndex { epochSeconds }

        private var isIteratorCreated = false

        override val version: Int
            get() = 1

        private inline fun Cursor.columnIndex(block: RecordTable.() -> String): Int {
            return getColumnIndex(block(RecordTable))
        }

        override fun recycle() {
            return cursor.close()
        }

        override fun iterator(): SerializableIterator {
            if (isIteratorCreated) {
                throw IllegalStateException("Iterator was already created")
            }

            isIteratorCreated = true

            return object : SerializableIterator {
                private var currentScore = 0
                private var currentEpochSeconds = 0L
                private val expressionBuffer = CharArrayBuffer(32)
                private val meaningBuffer = CharArrayBuffer(64)
                private val additionalNotesBuffer = CharArrayBuffer(16)

                init {
                    // Make the iterator reusable
                    moveToPosition(-1)
                }

                override fun moveToNext() = cursor.moveToNext()

                override fun initCurrent() {
                    cursor.run {
                        currentScore = getInt(scoreIndex)
                        currentEpochSeconds = getLong(epochSecondsIndex)

                        copyStringToBuffer(exprIndex, expressionBuffer)
                        copyStringToBuffer(meaningIndex, meaningBuffer)
                        copyStringToBuffer(additionalNotesIndex, additionalNotesBuffer)
                    }
                }

                override fun getCurrentElementByteSize(): Int {
                    return with(BinarySize) {
                        int32 /* score */ +
                                int64 /* epochSeconds */ +
                                stringUtf16(expressionBuffer) +
                                stringUtf16(meaningBuffer) +
                                stringUtf16(additionalNotesBuffer)
                    }
                }

                override fun writeCurrentElement(writer: ValueWriter) {
                    writer.run {
                        stringUtf16(expressionBuffer)
                        stringUtf16(meaningBuffer)
                        stringUtf16(additionalNotesBuffer)
                        int32(currentScore)
                        int64(currentEpochSeconds)
                    }
                }

                private fun ValueWriter.stringUtf16(buffer: CharArrayBuffer) {
                    stringUtf16(buffer.data, 0, buffer.sizeCopied)
                }
            }
        }
    }
}