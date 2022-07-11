package io.github.pelmenstar1.digiDict.data

import android.database.CharArrayBuffer
import android.database.Cursor
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import io.github.pelmenstar1.digiDict.serialization.*
import io.github.pelmenstar1.digiDict.require
import io.github.pelmenstar1.digiDict.validate

@Entity(tableName = "records")
data class Record(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = RecordTable.id)
    val id: Int,
    @ColumnInfo(name = RecordTable.expression) val expression: String,
    @ColumnInfo(name = RecordTable.meaning) val rawMeaning: String,
    @ColumnInfo(name = RecordTable.additionalNotes) val additionalNotes: String,
    @ColumnInfo(name = RecordTable.score) val score: Int,
    // in UTC
    @ColumnInfo(name = RecordTable.epochSeconds) val epochSeconds: Long,
) {
    init {
        require(id >= 0, "Id can't be negative")
        require(epochSeconds >= 0, "Epoch seconds can't be negative")
    }

    companion object {
        val SERIALIZER = object : BinarySerializer<Record> {
            override fun getByteSize(value: Record): Int {
                return with(BinarySize) {
                    int32 /* id */ +
                            int32 /* score */ +
                            int64 /* epochSeconds */ +
                            stringUtf16(value.expression) +
                            stringUtf16(value.rawMeaning) +
                            stringUtf16(value.additionalNotes)
                }
            }

            override fun writeTo(writer: ValueWriter, value: Record) {
                writer.run {
                    int32(value.id)
                    stringUtf16(value.expression)
                    stringUtf16(value.rawMeaning)
                    stringUtf16(value.additionalNotes)
                    int32(value.score)
                    int64(value.epochSeconds)
                }
            }

            override fun readFrom(reader: ValueReader): Record {
                val id = reader.int32()
                val expression = reader.stringUtf16()
                val rawMeaning = reader.stringUtf16()
                val additionalNotes = reader.stringUtf16()
                val score = reader.int32()
                val epochSeconds = reader.int64()

                validate(id >= 0, "Id can't be negative")
                validate(epochSeconds >= 0, "Epoch seconds can't be negative")

                return Record(
                    id,
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

fun Cursor.asRecordSerializableIterable(): SerializableIterable {
    val size = count
    val cursor = this

    return object : SerializableIterable {
        override val size: Int
            get() = size

        private val idIndex = columnIndex { id }
        private val exprIndex = columnIndex { expression }
        private val meaningIndex = columnIndex { meaning }
        private val additionalNotesIndex = columnIndex { additionalNotes }
        private val scoreIndex = columnIndex { score }
        private val epochSecondsIndex = columnIndex { epochSeconds }

        private var iteratorCreated = false

        private inline fun Cursor.columnIndex(block: RecordTable.() -> String): Int {
            return getColumnIndex(block(RecordTable))
        }

        override fun recycle() {
            return cursor.close()
        }

        override fun iterator(): SerializableIterator {
            if (iteratorCreated) {
                throw IllegalStateException("Iterator was already created")
            }

            iteratorCreated = true

            return object : SerializableIterator {
                private var currentId = 0
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
                        currentId = getInt(idIndex)
                        currentScore = getInt(scoreIndex)
                        currentEpochSeconds = getLong(epochSecondsIndex)

                        copyStringToBuffer(exprIndex, expressionBuffer)
                        copyStringToBuffer(meaningIndex, meaningBuffer)
                        copyStringToBuffer(additionalNotesIndex, additionalNotesBuffer)
                    }
                }

                override fun getCurrentElementByteSize(): Int {
                    return with(BinarySize) {
                        int32 /* id */ +
                                int32 /* score */ +
                                int64 /* epochSeconds */ +
                                stringUtf16(expressionBuffer) +
                                stringUtf16(meaningBuffer) +
                                stringUtf16(additionalNotesBuffer)
                    }
                }

                override fun writeCurrentElement(writer: ValueWriter) {
                    writer.run {
                        int32(currentId)
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