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
                epochSeconds == other.epochSeconds
    }

    override fun hashCode(): Int {
        var result = id
        result = result * 31 + expression.hashCode()
        result = result * 31 + rawMeaning.hashCode()
        result = result * 31 + additionalNotes.hashCode()
        result = result * 31 + score
        result = result * 31 + epochSeconds.hashCode()

        return result
    }

    override fun toString(): String {
        return "Record(id=$id, expression='$expression', rawMeaning='$rawMeaning', additionalNotes=${additionalNotes}, score=$score, epochSeconds=$epochSeconds)"
    }

    companion object {
        val EXPRESSION_COMPARATOR = Comparator<Record> { a, b ->
            a.expression.compareTo(b.expression)
        }

        val NO_ID_SERIALIZER = object : BinarySerializer<Record> {
            override fun getByteSize(value: Record): Int {
                return with(BinarySize) {
                    int32 /* score */ +
                            int64 /* epochSeconds */ +
                            stringUtf16(value.expression) +
                            stringUtf16(value.rawMeaning) +
                            stringUtf16(value.additionalNotes)
                }
            }

            override fun writeTo(writer: ValueWriter, value: Record) {
                writer.run {
                    stringUtf16(value.expression)
                    stringUtf16(value.rawMeaning)
                    stringUtf16(value.additionalNotes)
                    int32(value.score)
                    int64(value.epochSeconds)
                }
            }

            override fun readFrom(reader: ValueReader): Record {
                val expression = reader.stringUtf16()
                val rawMeaning = reader.stringUtf16()
                val additionalNotes = reader.stringUtf16()
                val score = reader.int32()
                val epochSeconds = reader.int64()

                if (epochSeconds < 0) {
                    throw ValidationException("Epoch seconds can't be negative")
                }

                return Record(
                    id = 0,
                    expression, rawMeaning, additionalNotes,
                    score,
                    epochSeconds
                )
            }

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