package io.github.pelmenstar1.digiDict.common.binarySerialization

import io.github.pelmenstar1.digiDict.common.ProgressReporter
import io.github.pelmenstar1.digiDict.common.trackLoopProgressWith
import java.io.IOException
import java.io.InputStream

class PrimitiveValueReader(private val input: InputStream) {
    private var byteBuffer = ByteArray(SMALL_BUFFER_SIZE)
    private var charBuffer: CharArray? = null

    private fun readInt16Internal(buffer: ByteArray, offset: Int): Int {
        return buffer[offset].toInt() and 0xFF or
                (buffer[offset + 1].toInt() and 0xFF shl 8)
    }

    private fun readInt16AsPrimitive() = readPrimitive(byteCount = 2) { readInt16Internal(it, 0) }

    fun int32() = readPrimitive(byteCount = 4) { buffer ->
        buffer[0].toInt() and 0xFF or
                (buffer[1].toInt() and 0xFF shl 8) or
                (buffer[2].toInt() and 0xFF shl 16) or
                (buffer[3].toInt() and 0xFF shl 24)
    }

    fun int64() = readPrimitive(byteCount = 8) { buffer ->
        buffer[0].toLong() and 0xffL or
                (buffer[1].toLong() and 0xffL shl 8) or
                (buffer[2].toLong() and 0xffL shl 16) or
                (buffer[3].toLong() and 0xffL shl 24) or
                (buffer[4].toLong() and 0xffL shl 32) or
                (buffer[5].toLong() and 0xffL shl 40) or
                (buffer[6].toLong() and 0xffL shl 48) or
                (buffer[7].toLong() and 0xffL shl 56)
    }

    private inline fun <T> readPrimitive(byteCount: Int, readFunc: (ByteArray) -> T): T {
        val buffer = byteBuffer
        readExact(buffer, byteCount)

        return readFunc(buffer)
    }

    fun stringUtf16(): String {
        val charLength = readInt16AsPrimitive() and 0xFFFF
        val byteLength = charLength * 2

        if (charLength == 0) {
            return ""
        }

        var bb = byteBuffer
        if (byteLength > bb.size) {
            bb = ByteArray(byteLength)
            byteBuffer = bb
        }

        var cb = charBuffer
        if (cb == null || charLength > cb.size) {
            cb = CharArray(charLength)
            charBuffer = cb
        }

        readExact(bb, byteLength)

        var charOffset = 0
        var byteOffset = 0

        while (charOffset < charLength) {
            cb[charOffset] = readInt16Internal(bb, byteOffset).toChar()

            charOffset++
            byteOffset += 2
        }

        return String(cb, 0, charLength)
    }

    @Suppress("UNCHECKED_CAST")
    fun <T : Any> array(serializer: BinarySerializer<out T>, progressReporter: ProgressReporter? = null): Array<T> {
        val size = int32()
        val result = serializer.newArrayOfNulls(size) as Array<T>

        trackLoopProgressWith(progressReporter, size) { i ->
            result[i] = serializer.readFrom(this)
        }

        return result
    }

    private fun readExact(buffer: ByteArray, n: Int) {
        var offset = 0
        while (offset < n) {
            val bytesRead = input.read(buffer, offset, n - offset)
            if (bytesRead <= 0) {
                throw IOException("Not enough data")
            }

            offset += bytesRead
        }
    }

    companion object {
        private const val SMALL_BUFFER_SIZE = 32
    }
}