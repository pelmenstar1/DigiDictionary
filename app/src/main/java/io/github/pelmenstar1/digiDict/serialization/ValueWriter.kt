package io.github.pelmenstar1.digiDict.serialization

import java.nio.ByteBuffer
import java.nio.ByteOrder

sealed class ValueWriter {
    abstract var offset: Int

    abstract fun int16(value: Short)
    abstract fun int32(value: Int)
    abstract fun int64(value: Long)
    abstract fun stringUtf16(value: String)
    abstract fun stringUtf16(chars: CharArray, start: Int, end: Int)

    fun char(c: Char) {
        int16(c.code.toShort())
    }

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

        override fun int16(value: Short) {
            writeInternal(2) { buffer, offset ->
                buffer[offset] = value.toByte()
                buffer[offset + 1] = (value.toInt() shr 8).toByte()
            }
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
            for (c in value) {
                char(c)
            }

            writeInternal(1) { buffer, offset -> buffer[offset] = 0 }
        }

        override fun stringUtf16(chars: CharArray, start: Int, end: Int) {
            for (i in start until end) {
                char(chars[i])
            }

            writeInternal(1) { buffer, offset -> buffer[offset] = 0 }
        }
    }

    private class ByteBufferImpl(private val buffer: ByteBuffer) : ValueWriter() {
        override var offset: Int
            get() = buffer.position()
            set(value) {
                buffer.position(value)
            }

        init {
            buffer.order(ByteOrder.LITTLE_ENDIAN)
        }

        override fun int16(value: Short) {
            buffer.putShort(value)
        }

        override fun int32(value: Int) {
            buffer.putInt(value)
        }

        override fun int64(value: Long) {
            buffer.putLong(value)
        }

        override fun stringUtf16(value: String) {
            for (c in value) {
                buffer.putChar(c)
            }

            buffer.put(0)
        }

        override fun stringUtf16(chars: CharArray, start: Int, end: Int) {
            for (i in start until end) {
                buffer.putChar(chars[i])
            }

            buffer.put(0)
        }
    }

    companion object {
        fun of(value: ByteArray): ValueWriter = ByteArrayImpl(value)
        fun of(value: ByteBuffer): ValueWriter = ByteBufferImpl(value)
    }
}