package io.github.pelmenstar1.digiDict.common.serialization

import io.github.pelmenstar1.digiDict.common.ProgressReporter
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.CharBuffer

/**
 * Encapsulates writing primitive types to [ByteBuffer].
 *
 * For this to work correctly, position of the [ByteBuffer] instance must be aligned by 2.
 * Also, the [ValueReader] instance takes ownership under the buffer, so the position, limit shouldn't be changed.
 */
class ValueReader(private val buffer: ByteBuffer) {
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

    fun int32() = buffer.int
    fun int64() = buffer.long

    fun stringUtf16(): String {
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
}