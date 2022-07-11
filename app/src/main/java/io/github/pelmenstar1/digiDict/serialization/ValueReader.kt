package io.github.pelmenstar1.digiDict.serialization

import io.github.pelmenstar1.digiDict.utils.indexOf
import io.github.pelmenstar1.digiDict.validate
import java.nio.ByteBuffer
import java.nio.ByteOrder

sealed class ValueReader {
    abstract var offset: Int

    abstract fun int32(): Int
    abstract fun int64(): Long
    abstract fun stringUtf16(): String

    /**
     * Only after returned [Sequence] is fully iterated, read-method of [ValueReader] can be used again.
     * Otherwise, result is undefined.
     */
    fun <T : Any> sequence(serializer: BinarySerializer<T>): Sequence<T> {
        val length = int32()

        validate(length >= 0,"Sequence length can't be negative")

        return object : Sequence<T> {
            private var iteratorCreated = false

            override fun iterator(): Iterator<T> {
                if (iteratorCreated) {
                    throw IllegalStateException("Sequence can be iterated only once")
                }

                iteratorCreated = true

                return object : Iterator<T> {
                    private var index = 0

                    override fun hasNext() = index < length

                    override fun next(): T {
                        val value = serializer.readFrom(this@ValueReader)
                        index++

                        return value
                    }
                }
            }
        }
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
            // Step is 2, because string is encoded as UTF-16
            val terminatorIndex = buffer.indexOf(0, offset, buffer.size, step = 2)

            // If there's no \0 symbol, then the data is invalid
            if (terminatorIndex == -1) {
                throw IllegalStateException("Invalid format")
            }

            val byteLength = terminatorIndex - offset

            // Avoids an allocation and a native call.
            if (byteLength == 0) {
                return ""
            }

            return readInternal(byteLength + 1) { buffer, offset ->
                String(buffer, offset, byteLength, Charsets.UTF_16LE)
            }
        }
    }

    private class ByteBufferImpl(private val buffer: ByteBuffer) : ValueReader() {
        override var offset: Int
            get() = buffer.position()
            set(value) {
                buffer.position(value)
            }

        init {
            buffer.order(ByteOrder.LITTLE_ENDIAN)
        }

        override fun int32() = buffer.int
        override fun int64() = buffer.long

        override fun stringUtf16(): String {
            val oldLimit = buffer.limit()

            val terminatorIndex = buffer.indexOf(0, step = 2)

            // If there's no \0 symbol, then the data is invalid
            if (terminatorIndex == -1) {
                throw IllegalStateException("Invalid format")
            }

            // Avoids an allocation and a native call.
            if (terminatorIndex == buffer.position()) {
                return ""
            }

            buffer.limit(terminatorIndex)

            val result = buffer.asCharBuffer().toString()

            buffer.limit(oldLimit)
            buffer.position(terminatorIndex + 1)

            return result
        }
    }

    companion object {
        fun of(value: ByteArray): ValueReader = ByteArrayImpl(value)
        fun of(value: ByteBuffer): ValueReader = ByteBufferImpl(value)
    }
}