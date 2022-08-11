package io.github.pelmenstar1.digiDict.common.serialization

import io.github.pelmenstar1.digiDict.common.ProgressReporter
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.CharBuffer

sealed class ValueReader {
    abstract var offset: Int

    abstract fun int32(): Int
    abstract fun int64(): Long
    abstract fun stringUtf16(): String

    @Suppress("UNCHECKED_CAST")
    fun <T : Any> array(serializer: BinarySerializer<out T>, progressReporter: ProgressReporter? = null): Array<T> {
        val n = int32()
        val result = serializer.newArrayOfNulls(n) as Array<T>

        for (i in 0 until n) {
            result[i] = serializer.readFrom(this)

            progressReporter?.onProgress(i, n)
        }

        return result
    }

    private class ByteArrayImpl(private val buffer: ByteArray) : ValueReader() {
        override var offset: Int = 0

        /**
         * Convenient method for reading value [T] and moving offset forward for [size] bytes.
         */
        private inline fun <T> readInternal(
            size: Int,
            block: (buffer: ByteArray, offset: Int) -> T
        ): T {
            val o = offset
            val value = block(buffer, o)
            offset = o + size

            return value
        }

        override fun int32(): Int {
            return readInternal(4) { buffer, offset ->
                buffer[offset].toInt() and 0xFF or
                        (buffer[offset + 1].toInt() and 0xFF shl 8) or
                        (buffer[offset + 2].toInt() and 0xFF shl 16) or
                        (buffer[offset + 3].toInt() and 0xFF shl 24)
            }
        }

        override fun int64(): Long {
            return readInternal(8) { buffer, offset ->
                buffer[offset].toLong() and 0xffL or
                        (buffer[offset + 1].toLong() and 0xffL shl 8) or
                        (buffer[offset + 2].toLong() and 0xffL shl 16) or
                        (buffer[offset + 3].toLong() and 0xffL shl 24) or
                        (buffer[offset + 4].toLong() and 0xffL shl 32) or
                        (buffer[offset + 5].toLong() and 0xffL shl 40) or
                        (buffer[offset + 6].toLong() and 0xffL shl 48) or
                        (buffer[offset + 7].toLong() and 0xffL shl 56)
            }
        }

        override fun stringUtf16(): String {
            val buf = buffer
            var o = offset

            // Read unsigned int16
            val strLength = (buf[o].toInt() or (buf[o + 1].toInt() shl 8)) and 0xFFFF
            o += 2

            // Avoids an allocation and a native call.
            if (strLength == 0) {
                offset = o

                return ""
            }

            val byteLength = strLength * 2

            val result = String(buf, o, byteLength, Charsets.UTF_16LE)
            offset = o + byteLength

            return result
        }
    }

    private class ByteBufferImpl(private val buffer: ByteBuffer) : ValueReader() {
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

        override fun int32() = buffer.int
        override fun int64() = buffer.long

        override fun stringUtf16(): String {
            val bb = buffer

            val strLength = bb.short.toInt() and 0xFFFF
            val bbPos = bb.position()

            return if (strLength == 0) {
                ""
            } else {
                val cb = charBuffer

                val tempBuffer = CharArray(strLength)

                cb.position(bbPos / 2 - initialCharBufferPosition)
                cb.get(tempBuffer, 0, strLength)

                bb.position(bbPos + strLength * 2)

                String(tempBuffer, 0, strLength)
            }
        }
    }

    companion object {
        fun of(value: ByteArray): ValueReader = ByteArrayImpl(value)

        /**
         * Returns [ValueWriter] instance with wrapped [ByteBuffer].
         *
         * For this to work correctly, position of the [ByteBuffer] instance must be aligned by 2.
         * Also, the [ValueReader] instance takes ownership under the buffer, so the position, limit shouldn't be changed.
         */
        fun of(value: ByteBuffer): ValueReader = ByteBufferImpl(value)
    }
}