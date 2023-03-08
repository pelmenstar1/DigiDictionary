package io.github.pelmenstar1.digiDict.common.binarySerialization

import io.github.pelmenstar1.digiDict.common.*
import java.io.InputStream
import java.nio.*
import kotlin.math.min

/**
 * Provides means for reading primitive types (like [Int], [Long], [String]) from [InputStream].
 * The byte order can't be changed and is always little-endian.
 *
 * The class provides consume-like methods that read a value and move the cursor forward for the amount of bytes that was read.
 * Because of its stream nature, there's no going back and the cursor can't be moved forward or backward.
 *
 * The reader is already optimized for buffer reading, thus a reasonable buffer size should be specified in constructor.
 */
class PrimitiveValueReader(private val inputStream: InputStream, bufferSize: Int) {
    private val byteBuffer = ByteArray(bufferSize)

    // Stores the amount of bytes that was consumed (read) in byteBuffer. The value should be even.
    private var consumedByteLength = 0

    // Stores actual byte length of byteBuffer.
    // If there was no call to inputStream.read and buffer is effectively zeroed, it's -1 (default value).
    private var actualBufferLength = -1

    // A cached reference to the char array which is used in consumeStringUtf8/16() to write a char data to it and create a string using this array.
    // The size of the array is only extended when a string with bigger length than the array's one is requested.
    // Outside the consumeStringUtf8/16() method, the array's content should be considered as garbage.
    private var charBuffer: CharArray? = null

    private var byteBufferForStrings: ByteArray? = null

    fun consumeByte(): Byte {
        invalidateBufferIfNecessary(minLength = 1)

        return byteBuffer[consumedByteLength++]
    }

    fun consumeShort(): Short {
        return consumeNumberPrimitiveInLong(byteCount = 2).toShort()
    }

    private fun consumeShortAsUnsignedInt(): Int {
        return consumeShort().toInt() and 0xFFFF
    }

    fun consumeInt() = consumeNumberPrimitiveInLong(byteCount = 4).toInt()
    fun consumeLong() = consumeNumberPrimitiveInLong(byteCount = 8)

    private fun consumeNumberPrimitiveInLong(byteCount: Int): Long {
        // Locals should be assigned after buffer invalidation.
        invalidateBufferIfNecessary(minLength = byteCount)

        val bb = byteBuffer
        val bufSize = bb.size
        val input = inputStream
        var actualBufLength = actualBufferLength
        var consumedBytes = consumedByteLength
        var result: Long

        val remBytes = actualBufLength - consumedBytes

        // Checks whether a primitive can be read from byteBuffer with additional read from InputStream.
        // It's only possible when amount of unconsumed bytes is greater than or equals to count of bytes needed for storing the primitive.
        if (remBytes >= byteCount) {
            result = withByteRegion(0L, bb, consumedBytes, 0, byteCount)

            consumedBytes += byteCount
        } else {
            // Here we write to the result what we have (remaining bytes in the buffer), fulfill the buffer again and
            // write the high bits from the position where we ended.
            val suffixLength = byteCount - remBytes

            result = withByteRegion(0L, bb, bufferStart = consumedBytes, valueStart = 0, length = remBytes)
            actualBufLength = input.readAtLeast(bb, offset = 0, minLength = suffixLength, maxLength = bufSize)
            result = withByteRegion(result, bb, bufferStart = 0, valueStart = remBytes, length = suffixLength)

            consumedBytes = suffixLength
        }

        consumedByteLength = consumedBytes
        actualBufferLength = actualBufLength

        return result
    }

    fun consumeStringUtf8(): String {
        val utf8ByteLength = consumeShortAsUnsignedInt()

        // It saves a couple of allocations.
        if (utf8ByteLength == 0) {
            return ""
        }

        val buf = byteBuffer
        val bufSize = buf.size
        var actualBufLength = actualBufferLength
        var consumedBytes = consumedByteLength
        val remBytesInBuf = actualBufLength - consumedBytes

        val result: String

        if (utf8ByteLength < remBytesInBuf) {
            result = String(buf, consumedBytes, utf8ByteLength, Charsets.UTF_8)
            consumedBytes += utf8ByteLength
        } else {
            var bufForStrings = byteBufferForStrings
            if (bufForStrings == null || utf8ByteLength > bufForStrings.size) {
                bufForStrings = ByteArray(utf8ByteLength)
                byteBufferForStrings = bufForStrings
            }

            System.arraycopy(buf, consumedBytes, bufForStrings, 0, remBytesInBuf)
            var remBytesToRead = utf8ByteLength - remBytesInBuf
            var offset = remBytesInBuf

            while (remBytesToRead >= bufSize) {
                inputStream.readExact(bufForStrings, offset, length = bufSize)

                offset += bufSize
                remBytesToRead -= bufSize
            }

            if (remBytesToRead > 0) {
                actualBufLength = inputStream.readAtLeast(
                    buf,
                    offset = 0,
                    minLength = remBytesToRead,
                    maxLength = bufSize
                )

                System.arraycopy(buf, 0, bufForStrings, offset, remBytesToRead)

                consumedBytes = remBytesToRead
            } else {
                consumedBytes = 0

                // Buffer must be invalidated on the next read.
                actualBufLength = -1
            }

            result = String(bufForStrings, 0, utf8ByteLength, Charsets.UTF_8)
        }

        consumedByteLength = consumedBytes
        actualBufferLength = actualBufLength

        return result
    }

    fun consumeStringUtf16(): String {
        val charLength = consumeShortAsUnsignedInt()

        // It saves a couple of allocations.
        if (charLength == 0) {
            return ""
        }

        var cb = charBuffer
        if (cb == null || charLength > cb.size) {
            cb = CharArray(charLength)
            charBuffer = cb
        }

        for (i in 0 until charLength) {
            cb[i] = consumeShort().toInt().toChar()
        }

        return String(cb, 0, charLength)
    }

    fun consumeIntArray(dest: IntArray, start: Int, end: Int) {
        for (i in start until end) {
            dest[i] = consumeInt()
        }
    }

    fun consumeIntArray(length: Int): IntArray {
        return IntArray(length).also { consumeIntArray(it, 0, length) }
    }

    fun consumeIntArrayAndLength(): IntArray {
        val length = consumeInt()

        return consumeIntArray(length)
    }

    @Suppress("UNCHECKED_CAST")
    fun <T : Any> consumeArray(
        serializer: BinarySerializer<out T>,
        compatInfo: BinarySerializationCompatInfo,
        progressReporter: ProgressReporter? = null
    ): Array<T> {
        val size = consumeInt()
        val result = serializer.newArrayOfNulls(size) as Array<T>

        trackLoopProgressWith(progressReporter, size) { i ->
            result[i] = serializer.readFrom(this, compatInfo)
        }

        return result
    }

    private fun invalidateBufferIfNecessary(minLength: Int) {
        val bb = byteBuffer
        val bufSize = bb.size

        // The buffer should be invalidated when either no data was written to it or all the bytes in the buffer is consumed.
        if (actualBufferLength < 0 || consumedByteLength == bufSize) {
            actualBufferLength = inputStream.readAtLeast(
                bb,
                0,
                minLength = min(minLength, bufSize),
                maxLength = bufSize
            )
            consumedByteLength = 0
        }
    }

    companion object {
        internal fun withByteRegion(
            initial: Long,
            buffer: ByteArray,
            bufferStart: Int,
            valueStart: Int,
            length: Int
        ): Long {
            var bufferIndex = bufferStart

            var shift = valueStart shl 3
            var result = initial

            repeat(length) {
                val b = buffer[bufferIndex++]
                result = result or ((b.toLong() and 0xFFL) shl shift)

                shift += 8
            }

            return result
        }
    }
}