package io.github.pelmenstar1.digiDict.common.serialization

import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.CharBuffer

sealed class ValueWriter {
    abstract var offset: Int

    abstract fun int32(value: Int)
    abstract fun int64(value: Long)
    abstract fun stringUtf16(value: String)
    abstract fun stringUtf16(chars: CharArray, start: Int, end: Int)

    private class ByteArrayImpl(private val buffer: ByteArray) : ValueWriter() {
        override var offset: Int = 0

        /**
         * Convenient method for writing value and moving offset forward for [size] bytes.
         */
        private inline fun writeInternal(
            size: Int,
            block: (buffer: ByteArray, offset: Int) -> Unit
        ) {
            val o = offset
            block(buffer, o)
            offset = o + size
        }

        override fun int32(value: Int) {
            writeInternal(4) { buffer, offset ->
                buffer[offset] = value.toByte()
                buffer[offset + 1] = (value shr 8).toByte()
                buffer[offset + 2] = (value shr 16).toByte()
                buffer[offset + 3] = (value shr 24).toByte()
            }
        }

        override fun int64(value: Long) {
            writeInternal(8) { buffer, offset ->
                buffer[offset] = value.toByte()
                buffer[offset + 1] = (value shr 8).toByte()
                buffer[offset + 2] = (value shr 16).toByte()
                buffer[offset + 3] = (value shr 24).toByte()
                buffer[offset + 4] = (value shr 32).toByte()
                buffer[offset + 5] = (value shr 40).toByte()
                buffer[offset + 6] = (value shr 48).toByte()
                buffer[offset + 7] = (value shr 56).toByte()
            }
        }

        override fun stringUtf16(value: String) {
            writeStringUtf16Internal(value::get, 0, value.length)
        }

        override fun stringUtf16(chars: CharArray, start: Int, end: Int) {
            writeStringUtf16Internal(chars::get, start, end)
        }

        private inline fun writeStringUtf16Internal(getChar: (Int) -> Char, start: Int, end: Int) {
            val length = end - start

            checkStringLength(length)
            writeInt16(length)

            for (i in start until end) {
                val value = getChar(i).code

                writeInt16(value)
            }
        }

        private fun writeInt16(value: Int) {
            writeInternal(2) { buffer, offset ->
                buffer[offset] = value.toByte()
                buffer[offset + 1] = (value shr 8).toByte()
            }
        }
    }

    private class ByteBufferImpl(private val buffer: ByteBuffer) : ValueWriter() {
        override var offset: Int
            get() = buffer.position()
            set(value) {
                buffer.position(value)
            }

        private val charBuffer: CharBuffer
        private val initialCharBufferPosition: Int

        init {
            val bbPos = buffer.position()

            if (bbPos % 2 != 0) {
                throw IllegalArgumentException("buffer.position() must be aligned by 2")
            }

            buffer.order(ByteOrder.LITTLE_ENDIAN)
            charBuffer = buffer.asCharBuffer()
            initialCharBufferPosition = bbPos / 2
        }

        override fun int32(value: Int) {
            buffer.putInt(value)
        }

        override fun int64(value: Long) {
            buffer.putLong(value)
        }

        override fun stringUtf16(value: String) {
            val valueLength = value.length
            checkStringLength(valueLength)

            buffer.also {
                it.putShort(valueLength.toShort())

                for (i in 0 until valueLength) {
                    it.putChar(value[i])
                }
            }
        }

        override fun stringUtf16(chars: CharArray, start: Int, end: Int) {
            val length = end - start
            checkStringLength(length)

            val bb = buffer
            val cb = charBuffer

            bb.putShort(length.toShort())
            val bbPos = bb.position()

            cb.position(bbPos / 2 - initialCharBufferPosition)
            cb.put(chars, start, length)

            bb.position(bbPos + length * 2)
        }
    }

    companion object {
        fun of(value: ByteArray): ValueWriter = ByteArrayImpl(value)

        /**
         * Returns [ValueWriter] instance with wrapped [ByteBuffer]. For this to work correctly,
         * position of the [ByteBuffer] instance must be aligned by 2.
         */
        fun of(value: ByteBuffer): ValueWriter = ByteBufferImpl(value)

        internal fun checkStringLength(length: Int) {
            if (length > 65535) {
                throw IllegalArgumentException("Length of the string to be written can't be greater than 65535")
            }
        }
    }
}