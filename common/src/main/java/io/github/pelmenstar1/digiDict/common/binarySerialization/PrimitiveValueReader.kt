package io.github.pelmenstar1.digiDict.common.binarySerialization

import io.github.pelmenstar1.digiDict.common.*
import java.io.InputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.CharBuffer
import kotlin.math.min

/**
 * Provides means for reading primitive types (like [Int], [Long], [String]) from [InputStream].
 * The byte order can't be changed and is always little-endian.
 *
 * The class provides consume-like methods that read a value and move the cursor forward for the amount of bytes that was read.
 * Because of its stream nature, there's no going back and the cursor can't be moved forward or backward.
 *
 * The reader is already optimized for buffer reading, thus a reasonable buffer size should be specified in constructor.
 * There are several limitations imposed on buffer size:
 * - It should be greater than or equals to 8.
 * - It should be even.
 */
class PrimitiveValueReader(private val inputStream: InputStream, bufferSize: Int) {
    private val byteBufferArray = ByteArray(bufferSize)

    // Stores the amount of bytes that was consumed (read) in byteBuffer. The value should be even.
    private var consumedByteLength = 0

    // Stores actual byte length of byteBuffer.
    // If there was no call to inputStream.read and buffer is effectively zeroed, it's -1 (default value).
    private var actualBufferLength = -1

    // A cached reference to the char array which is used in consumeStringUtf16() to write a char data to it and create a string using this array.
    // The size of the array is only extended when a string with bigger length than the array's one is requested.
    // Outside the consumeStringUtf16() method, the array's content should be considered as garbage.
    private var charBuffer: CharArray? = null

    private var byteBuffer: ByteBuffer? = null
    private var byteBufferAsChar: CharBuffer? = null

    init {
        when {
            bufferSize < 8 -> throw IllegalArgumentException("bufferSize should be greater than or equals to 8")
            bufferSize % 2 != 0 -> throw IllegalArgumentException("bufferSize should be even")
        }
    }

    private fun getByteBuffer(): ByteBuffer {
        return getLazyValue(
            byteBuffer,
            {
                ByteBuffer.wrap(byteBufferArray).apply {
                    order(ByteOrder.LITTLE_ENDIAN)
                }
            },
            { byteBuffer = it }
        )
    }

    private fun getByteBufferAsChar(): CharBuffer {
        return getLazyValue(
            byteBufferAsChar,
            { getByteBuffer().asCharBuffer() },
            { byteBufferAsChar = it }
        )
    }

    fun consumeShort(): Short {
        // Short (2 bytes) consumption is special because as consumedByteLength should be even and buffer size is greater than or equals to 8,
        // there can't be cross-buffer read. The logic can be simpler comparing to general consumePrimitive.
        invalidateBufferIfNecessary(minLength = 2)

        val bb = byteBufferArray
        val consumedBytes = consumedByteLength

        val result = bb.readShort(consumedBytes)
        consumedByteLength = consumedBytes + 2

        return result
    }

    fun consumeInt() = consumeNumberPrimitive(byteCount = 4, zero = 0, ByteArray::readInt, Int::withByteRegionOnZero)
    fun consumeLong() =
        consumeNumberPrimitive(byteCount = 8, zero = 0L, ByteArray::readLong, Long::withByteRegionOnZero)

    private inline fun <T> consumeNumberPrimitive(
        byteCount: Int,
        zero: T,
        readValue: ByteArray.(offset: Int) -> T,
        withByteRegionOnZero: T.(buffer: ByteArray, bufferStart: Int, valueStart: Int, length: Int) -> T
    ): T {
        // Locals should be assigned after buffer invalidation.
        invalidateBufferIfNecessary(minLength = byteCount)

        val bb = byteBufferArray
        val bufSize = bb.size
        val input = inputStream
        var actualBufLength = actualBufferLength
        var consumedBytes = consumedByteLength
        var result = zero

        val remBytes = actualBufLength - consumedBytes

        // Checks whether a primitive can be read from byteBuffer with additional read from InputStream.
        // It's only possible when amount of unconsumed bytes is greater than or equals to count of bytes needed for storing the primitive.
        if (remBytes >= byteCount) {
            result = bb.readValue(consumedBytes)
            consumedBytes += byteCount
        } else {
            // Here we write to the result what we have (remaining bytes in the buffer), fulfill the buffer again and
            // write the high bits from the position where we ended.
            val suffixLength = byteCount - remBytes

            result = result.withByteRegionOnZero(bb, consumedBytes, 0, remBytes)
            actualBufLength = input.readAtLeast(bb, 0, minLength = suffixLength, maxLength = bufSize)
            result = result.withByteRegionOnZero(bb, 0, remBytes, suffixLength)

            consumedBytes = suffixLength
        }

        consumedByteLength = consumedBytes
        actualBufferLength = actualBufLength

        return result
    }

    fun consumeStringUtf16(): String {
        val charLength = consumeShort().toInt() and 0xFFFF

        // It saves a couple of allocations.
        if (charLength == 0) {
            return ""
        }

        var cb = charBuffer
        if (cb == null || charLength > cb.size) {
            cb = CharArray(charLength)
            charBuffer = cb
        }

        consumeCharArray(cb, 0, charLength)
        return String(cb, 0, charLength)
    }

    fun consumeCharArray(chars: CharArray, start: Int, end: Int) {
        val charLength = end - start
        val byteLength = charLength * 2

        // Locals should be assigned after buffer invalidation.
        invalidateBufferIfNecessary(minLength = byteLength)

        val bb = byteBufferArray
        val bbAsChar = getByteBufferAsChar()
        val bufSize = bb.size
        val bufSizeAsChar = bufSize / 2
        val input = inputStream
        var actualBufLength = actualBufferLength
        var consumedBytes = consumedByteLength
        var charPos = start

        val remCachedBytes = actualBufLength - consumedBytes

        val prefixByteLength = min(byteLength, remCachedBytes)
        val prefixCharLength = prefixByteLength / 2

        bbAsChar.position(consumedBytes / 2)
        bbAsChar.get(chars, start, prefixCharLength)
        charPos += prefixCharLength
        consumedBytes += prefixByteLength

        // If we can't read a full content of a string from existing buffer, we should read from InputStream to fulfill char buffer.
        if (charPos != end) {
            var remChars: Int

            while (true) {
                remChars = end - charPos

                // Bail out if we can't read a full byte buffer.
                if (remChars < bufSizeAsChar) {
                    break
                }

                input.readExact(bb, 0, bufSize)

                bbAsChar.position(0)
                bbAsChar.get(chars, charPos, bufSizeAsChar)

                charPos += bufSizeAsChar
            }

            // Read from InputStream if some chars are still remaining.
            if (remChars > 0) {
                val remCharsAsByte = remChars * 2
                actualBufLength = input.readAtLeast(bb, 0, minLength = remCharsAsByte, maxLength = bufSize)
                bbAsChar.position(0)
                bbAsChar.get(chars, charPos, remChars)

                consumedBytes = remCharsAsByte
            }
        }

        consumedByteLength = consumedBytes
        actualBufferLength = actualBufLength
    }

    @Suppress("UNCHECKED_CAST")
    fun <T : Any> consumeArray(
        serializer: BinarySerializer<out T>,
        progressReporter: ProgressReporter? = null
    ): Array<T> {
        val size = consumeInt()
        val result = serializer.newArrayOfNulls(size) as Array<T>

        trackLoopProgressWith(progressReporter, size) { i ->
            result[i] = serializer.readFrom(this)
        }

        return result
    }

    private fun invalidateBufferIfNecessary(minLength: Int) {
        val bb = byteBufferArray
        val bufSize = bb.size

        if (actualBufferLength < 0 || consumedByteLength == bufSize) {
            actualBufferLength =
                inputStream.readAtLeast(bb, 0, minLength = min(minLength, bufSize), maxLength = bufSize)
            consumedByteLength = 0
        }
    }
}