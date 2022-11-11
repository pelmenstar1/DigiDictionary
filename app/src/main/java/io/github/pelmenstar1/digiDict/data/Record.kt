package io.github.pelmenstar1.digiDict.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import io.github.pelmenstar1.digiDict.common.binarySerialization.BinarySerializerResolver
import io.github.pelmenstar1.digiDict.common.binarySerialization.checkDataValidity
import io.github.pelmenstar1.digiDict.common.equalsPattern
import kotlinx.serialization.Serializable

@Entity(
    tableName = "records",
    indices = [Index(RecordTable.expression, unique = true)]
)
@Serializable
open class Record(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = RecordTable.id)
    final override val id: Int = 0,
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
        val SERIALIZER_RESOLVER = BinarySerializerResolver<Record> {
            register<Record>(
                version = 1,
                write = { value ->
                    emit(value.expression)
                    emit(value.meaning)
                    emit(value.additionalNotes)
                    emit(value.score)
                    emit(value.epochSeconds)
                },
                read = {
                    val expression = consumeStringUtf16()
                    val meaning = consumeStringUtf16()
                    val additionalNotes = consumeStringUtf16()
                    val score = consumeInt()
                    val epochSeconds = consumeLong()

                    checkDataValidity("Epoch seconds can't be negative") { epochSeconds >= 0 }

                    Record(
                        id = 0,
                        expression, meaning, additionalNotes,
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
        return "RecordWithBadges(id=$id, expression='$expression', meaning='$meaning', additionalNotes='$additionalNotes', score=$score, epochSeconds=$epochSeconds, badges=${badges.contentToString()}"
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