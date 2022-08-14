package io.github.pelmenstar1.digiDict.data

import android.database.CharArrayBuffer
import android.database.Cursor
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import io.github.pelmenstar1.digiDict.common.serialization.*

@Entity(
    tableName = "records",
    indices = [Index(RecordTable.expression, unique = true)]
)
open class Record(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = RecordTable.id)
    val id: Int,
    @ColumnInfo(name = RecordTable.expression) val expression: String,
    @ColumnInfo(name = RecordTable.meaning) val rawMeaning: String,
    @ColumnInfo(name = RecordTable.additionalNotes) val additionalNotes: String,
    @ColumnInfo(name = RecordTable.score) val score: Int,
    // in UTC
    @ColumnInfo(name = RecordTable.epochSeconds) val epochSeconds: Long,
    @ColumnInfo(name = RecordTable.badges, defaultValue = "") var rawBadges: String = ""
) : EntityWithPrimaryKeyId<Record> {
    init {
        require(id >= 0) { "Id can't be negative" }
        require(epochSeconds >= 0) { "Epoch seconds can't be negative" }
    }

    override fun equals(other: Any?): Boolean {
        if (other === this) return true
        if (other == null || javaClass != other.javaClass) return false

        other as Record

        return id == other.id && equalsNoId(other)
    }

    override fun equalsNoId(other: Record): Boolean {
        return expression == other.expression &&
                rawMeaning == other.rawMeaning &&
                additionalNotes == other.additionalNotes &&
                score == other.score &&
                epochSeconds == other.epochSeconds &&
                rawBadges == other.rawBadges
    }

    override fun hashCode(): Int {
        var result = id
        result = result * 31 + expression.hashCode()
        result = result * 31 + rawMeaning.hashCode()
        result = result * 31 + additionalNotes.hashCode()
        result = result * 31 + score
        result = result * 31 + epochSeconds.hashCode()
        result = result * 31 + rawBadges.hashCode()

        return result
    }

    override fun toString(): String {
        return "Record(id=$id, expression='$expression', rawMeaning='$rawMeaning', additionalNotes=${additionalNotes}, score=$score, epochSeconds=$epochSeconds, badges=$rawBadges)"
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
                            stringUtf16(value.rawMeaning) +
                            stringUtf16(value.additionalNotes) +
                            int32 /* score */ +
                            int64 /* epochSeconds */
                },
                write = { value ->
                    stringUtf16(value.expression)
                    stringUtf16(value.rawMeaning)
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
                        epochSeconds,
                        rawBadges = ""
                    )
                }
            )

            forVersion<Record>(
                version = 2,
                getByteSize = { value ->
                    stringUtf16(value.expression) +
                            stringUtf16(value.rawMeaning) +
                            stringUtf16(value.additionalNotes) +
                            stringUtf16(value.rawBadges)
                    int32 /* score */ +
                            int64 /* epochSeconds */
                },
                write = { value ->
                    stringUtf16(value.expression)
                    stringUtf16(value.rawMeaning)
                    stringUtf16(value.additionalNotes)
                    stringUtf16(value.rawBadges)
                    int32(value.score)
                    int64(value.epochSeconds)
                },
                read = {
                    val expression = stringUtf16()
                    val rawMeaning = stringUtf16()
                    val additionalNotes = stringUtf16()
                    val rawBadges = stringUtf16()
                    val score = int32()
                    val epochSeconds = int64()

                    if (epochSeconds < 0) {
                        throw ValidationException("Epoch seconds can't be negative")
                    }

                    Record(
                        id = 0,
                        expression, rawMeaning, additionalNotes,
                        score,
                        epochSeconds,
                        rawBadges
                    )
                }
            )
        }
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
    const val badges = "badges"
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
        private val badgesIndex = columnIndex { badges }

        private var isIteratorCreated = false

        override val version: Int
            get() = 2

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
                private val badgesBuffer = CharArrayBuffer(32)

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
                        copyStringToBuffer(badgesIndex, badgesBuffer)
                    }
                }

                override fun getCurrentElementByteSize(): Int {
                    return with(BinarySize) {
                        int32 /* score */ +
                                int64 /* epochSeconds */ +
                                stringUtf16(expressionBuffer) +
                                stringUtf16(meaningBuffer) +
                                stringUtf16(additionalNotesBuffer) +
                                stringUtf16(badgesBuffer)
                    }
                }

                override fun writeCurrentElement(writer: ValueWriter) {
                    writer.run {
                        stringUtf16(expressionBuffer)
                        stringUtf16(meaningBuffer)
                        stringUtf16(additionalNotesBuffer)
                        stringUtf16(badgesBuffer)
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