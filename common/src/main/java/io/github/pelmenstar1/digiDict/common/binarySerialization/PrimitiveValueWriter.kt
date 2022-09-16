package io.github.pelmenstar1.digiDict.common.binarySerialization

import io.github.pelmenstar1.digiDict.common.*
import java.io.OutputStream
import java.nio.*

/**
 * Provides means for writing primitive types (like [Int], [Long], [String]) to [OutputStream].
 * The byte order can't be changed and is always little-endian.
 *
 * The class provides emit-like methods that write a value and move the cursor forward for the amount of bytes that was written.
 * Because of its stream nature, there's no going back and the cursor can't be moved forward or backward.
 *
 * The writer is already buffered, thus a reasonable buffer size should be specified in constructor.
 * There are several limitations imposed on buffer size:
 * - It should be greater than or equals to 8.
 * - It should be even.
 */
class PrimitiveValueWriter(private val output: OutputStream, bufferSize: Int) {
    private val byteBufferArray = ByteArray(bufferSize)

    private var byteBuffer: ByteBuffer? = null

    // A CharBuffer which can effectively write chars to byteBufferArray
    private var byteBufferAsCharHolder: CharBuffer? = null

    // A IntBuffer which can effectively write ints to byteBufferArray
    private var byteBufferAsIntHolder: IntBuffer? = null
    private var byteBufferAsIntOffset = 0

    // A cached reference to the char array which is used in emit(String) to write a char data to it and write the array to the stream.
    // There's a temporary step with the char array because CharBuffer (when mapped from ByteBuffer)
    // provides optimized way for writing CharArray but not String.
    //
    // The size of the array is only extended when a string with bigger length than the array's one is requested.
    // Outside the emit(String) method, the array's content should be considered as garbage.
    private var charBuffer: CharArray? = null

    // Stores a current position in byteBufferArray from which writing the data should start.
    // It can't equal to byteBufferArray.size and should be even.
    private var bufferPos = 0

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

    // offset is not used and its only purpose is to match a lambda signature in emitArray()
    private fun getByteBufferAsChar(@Suppress("UNUSED_PARAMETER") offset: Int = 0): CharBuffer {
        return getLazyValue(
            byteBufferAsCharHolder,
            { getByteBuffer().asCharBuffer() },
            { byteBufferAsCharHolder = it }
        )
    }

    private fun getByteBufferAsInt(offset: Int): IntBuffer {
        var bbAsInt = byteBufferAsIntHolder

        if (bbAsInt == null || byteBufferAsIntOffset != offset) {
            val bb = getByteBuffer()
            bb.position(offset)
            bbAsInt = bb.asIntBuffer()
        }

        byteBufferAsIntHolder = bbAsInt
        byteBufferAsIntOffset = offset

        return bbAsInt!!
    }

    fun emit(value: Short) {
        // Short (2 bytes) emission is special because as bufferPos should be even and buffer size is greater than or equals to 8,
        // there can't be cross-buffer writing. The logic can be simpler comparing to general emitPrimitive.
        val bp = bufferPos

        value.writeTo(byteBufferArray, bp)
        setBufferPosAndSyncIfNecessary(bp + 2)
    }

    fun emit(value: Int) {
        emitNumberPrimitive(value, byteCount = 4, Int::writeTo, Int::getByteAt)
    }

    fun emit(value: Long) {
        emitNumberPrimitive(value, byteCount = 8, Long::writeTo, Long::getByteAt)
    }

    private inline fun <T> emitNumberPrimitive(
        value: T,
        byteCount: Int,
        writeValue: T.(dest: ByteArray, offset: Int) -> Unit,
        getByteAt: T.(offset: Int) -> Byte
    ) {
        val buf = byteBufferArray
        var bp = bufferPos
        val remaining = buf.size - bp

        if (remaining >= byteCount) {
            value.writeValue(buf, bp)
            bp += byteCount
        } else {
            for (i in 0 until remaining) {
                buf[bp++] = value.getByteAt(i)
            }

            // If remaining < byteCount, it means that we can't write enough bits of value to the buffer and
            // it needs to be written to the stream first.
            output.write(buf, 0, bp)

            // After a buffer synchronization, the buffer position should be set to zero in order to continue writing of remaining
            // bits of value.
            bp = 0
            for (i in remaining until byteCount) {
                buf[bp++] = value.getByteAt(i)
            }
        }

        setBufferPosAndSyncIfNecessary(bp)
    }

    fun emit(value: String) {
        var cb = charBuffer
        val valueLength = value.length

        checkStringLength(valueLength)
        emit(valueLength.toShort())

        // It saves a few allocations if the string is empty.
        if (valueLength > 0) {
            if (cb == null || valueLength > cb.size) {
                cb = CharArray(valueLength)
                charBuffer = cb
            }

            value.toCharArray(cb)
            emit(cb, 0, valueLength)
        }
    }

    fun emit(chars: CharArray, start: Int, end: Int) {
        emitPrimitiveArray(chars, start, end, elementSize = 2, this::getByteBufferAsChar, CharBuffer::put)
    }

    fun emit(ints: IntArray, start: Int, end: Int) {
        emitPrimitiveArray(ints, start, end, elementSize = 4, this::getByteBufferAsInt, IntBuffer::put)
    }

    fun emitArrayAndLength(ints: IntArray, start: Int = 0, end: Int = ints.size) {
        val length = end - start

        emit(length)
        emit(ints, start, end)
    }

    private inline fun <TArray : Any, TBuffer : Buffer> emitPrimitiveArray(
        array: TArray,
        start: Int,
        end: Int,
        elementSize: Int,
        getElementBuffer: (offset: Int) -> TBuffer,
        putArray: TBuffer.(TArray, start: Int, length: Int) -> Unit
    ) {
        val out = output
        val bb = byteBufferArray
        var bp = bufferPos
        val bufSize = bb.size

        val elemPos = bp / elementSize
        val elemOffset = bp % elementSize
        val offsetElemBuffer = getElementBuffer(elemOffset)

        val elementLength = end - start

        offsetElemBuffer.position(elemPos)
        val remElementsInByteBuffer = (bufSize - bp) / elementSize

        if (remElementsInByteBuffer >= elementLength) {
            // We can write a whole region of elements.
            offsetElemBuffer.putArray(array, start, elementLength)
            bp += elementLength * elementSize
        } else {
            var elementPos = start

            // Write a prefix to fulfill the buffer and write it.
            offsetElemBuffer.putArray(array, elementPos, remElementsInByteBuffer)
            bp += remElementsInByteBuffer * elementSize
            elementPos += remElementsInByteBuffer

            out.write(bb, 0, bp)

            val elemBuffer = getElementBuffer(0)
            val bufSizeAsElement = bufSize / elementSize
            val alignedBufSize = bufSizeAsElement * elementSize

            // Stores amount of elements that need to be written.
            var remElements: Int

            while (true) {
                remElements = end - elementPos

                // Exit the loop if we can't do full buffer write.
                if (remElements < bufSizeAsElement) {
                    break
                }

                elemBuffer.position(0)
                elemBuffer.putArray(array, elementPos, bufSizeAsElement)

                elementPos += bufSizeAsElement
                out.write(bb, 0, alignedBufSize)
            }

            // TODO: Fix code duplication.
            // Write suffix if it exists.
            if (remElements != 0) {
                elemBuffer.position(0)
                elemBuffer.putArray(array, elementPos, remElements)

                bp = remElements * elementSize
            } else {
                bp = 0
            }
        }

        setBufferPosAndSyncIfNecessary(bp)
    }

    fun <T : Any> emit(
        values: Array<out T>,
        serializer: BinarySerializer<in T>,
        progressReporter: ProgressReporter? = null
    ) {
        val size = values.size
        emit(size)

        trackLoopProgressWith(progressReporter, size) { i ->
            serializer.writeTo(this, values[i])
        }
    }

    fun flush() {
        val bp = bufferPos
        if (bp > 0) {
            output.write(byteBufferArray, 0, bp)
        }
    }

    private fun setBufferPosAndSyncIfNecessary(newPos: Int) {
        val bb = byteBufferArray
        val bufSize = bb.size

        bufferPos = if (newPos == bufSize) {
            output.write(bb, 0, bufSize)

            0
        } else {
            newPos
        }
    }

    companion object {
        internal fun checkStringLength(length: Int) {
            if (length > 65535) {
                throw IllegalArgumentException("Length of the string to be written can't be greater than 65535")
            }
        }
    }
}