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
    private var byteBufferAsCharHolder: CharBuffer? = null
    private var byteBufferAsIntHolder: IntBuffer? = null

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
            byteBufferAsCharHolder,
            { getByteBuffer().asCharBuffer() },
            { byteBufferAsCharHolder = it }
        )
    }

    private fun getByteBufferAsInt(): IntBuffer {
        return getLazyValue(
            byteBufferAsIntHolder,
            { getByteBuffer().asIntBuffer() },
            { byteBufferAsIntHolder = it }
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

    fun consumeCharArray(dest: CharArray, start: Int, end: Int) {
        // Consumption of char array is always "aligned" as consumedByteLength is always even.
        consumePrimitiveArrayAligned(dest, start, end, elementSize = 2, getByteBufferAsChar(), CharBuffer::get)
    }

    fun consumeIntArray(dest: IntArray, start: Int, end: Int) {
        consumePrimitiveArray(
            dest,
            start,
            end,
            elementSize = 4,
            this::consumeIntArrayAligned,
            this::consumeIntArrayNonAligned
        )
    }

    private fun consumeIntArrayAligned(dest: IntArray, start: Int, end: Int) {
        consumePrimitiveArrayAligned(dest, start, end, elementSize = 4, getByteBufferAsInt(), IntBuffer::get)
    }

    private fun consumeIntArrayNonAligned(dest: IntArray, start: Int, end: Int) {
        consumePrimitiveArrayNonAligned(dest, start, end, IntArray::set, this::consumeInt)
    }

    fun consumeIntArray(length: Int): IntArray {
        return IntArray(length).also { consumeIntArray(it, 0, length) }
    }

    fun consumeIntArrayAndLength(): IntArray {
        val length = consumeInt()

        return consumeIntArray(length)
    }

    private inline fun <TArray> consumePrimitiveArray(
        dest: TArray,
        start: Int,
        end: Int,
        elementSize: Int,
        consumeAligned: (TArray, Int, Int) -> Unit,
        consumeNonAligned: (TArray, Int, Int) -> Unit,
    ) {
        if (consumedByteLength % elementSize == 0) {
            consumeAligned(dest, start, end)
        } else {
            consumeNonAligned(dest, start, end)
        }
    }

    /**
     * A builder for creating a function that consumes a primitive array
     * when [consumedByteLength] is not a multiple of count of bytes required to store [TValue].
     *
     * The performance of the implementation can be improved by using [IntBuffer]
     * and another things used in [consumePrimitiveArrayAligned] but the effort is probably not worth it.
     */
    private inline fun <TValue, TArray> consumePrimitiveArrayNonAligned(
        dest: TArray,
        start: Int,
        end: Int,
        setValue: TArray.(Int, TValue) -> Unit,
        consumePrimitive: () -> TValue
    ) {
        for (i in start until end) {
            dest.setValue(i, consumePrimitive())
        }
    }

    /**
     * A builder for creating a function that consumes a primitive array when [consumedByteLength] is a multiple of [elementSize].
     * If [consumedByteLength] is not a multiple of [elementSize], the result will be wrong.
     */
    private inline fun <TArray : Any, TBuffer : Buffer> consumePrimitiveArrayAligned(
        dest: TArray,
        start: Int,
        end: Int,
        elementSize: Int,
        elementBuffer: TBuffer,
        getArray: TBuffer.(TArray, start: Int, length: Int) -> Unit
    ) {
        val length = end - start
        val byteLength = length * elementSize

        // Locals should be assigned after buffer invalidation.
        invalidateBufferIfNecessary(minLength = byteLength)

        val bb = byteBufferArray
        val bufSize = bb.size
        val bufSizeAsElement = bufSize / elementSize
        val alignedBufSize = bufSizeAsElement * elementSize

        val input = inputStream
        var actualBufLength = actualBufferLength
        var consumedBytes = consumedByteLength
        var elemPos = start

        val remCachedBytes = actualBufLength - consumedBytes

        val prefixByteLength = min(byteLength, remCachedBytes)
        val prefixElementLength = prefixByteLength / elementSize

        elementBuffer.position(consumedBytes / elementSize)
        elementBuffer.getArray(dest, start, prefixElementLength)
        elemPos += prefixElementLength
        consumedBytes += prefixByteLength

        // If we can't read a full content of a string from existing buffer, we should read from InputStream to fulfill char buffer.
        if (elemPos != end) {
            var remElements: Int
            while (true) {
                remElements = end - elemPos

                // Bail out if we can't read a full byte buffer.
                if (remElements < bufSizeAsElement) {
                    break
                }

                input.readExact(bb, 0, alignedBufSize)

                elementBuffer.position(0)
                elementBuffer.getArray(dest, elemPos, bufSizeAsElement)

                elemPos += bufSizeAsElement
            }

            // Read from InputStream if some chars are still remaining.
            if (remElements > 0) {
                val remElementsAsByte = remElements * elementSize
                actualBufLength = input.readAtLeast(bb, 0, minLength = remElementsAsByte, maxLength = bufSize)

                elementBuffer.position(0)
                elementBuffer.getArray(dest, elemPos, remElements)

                consumedBytes = remElementsAsByte
            } else {
                consumedBytes = 0
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
}