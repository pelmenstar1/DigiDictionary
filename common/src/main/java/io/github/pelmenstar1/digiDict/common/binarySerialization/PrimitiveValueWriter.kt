package io.github.pelmenstar1.digiDict.common.binarySerialization

import io.github.pelmenstar1.digiDict.common.ProgressReporter
import io.github.pelmenstar1.digiDict.common.processUtf8Bytes
import io.github.pelmenstar1.digiDict.common.trackLoopProgressWith
import io.github.pelmenstar1.digiDict.common.utf8Size
import java.io.OutputStream

/**
 * Provides means for writing primitive types (like [Int], [Long], [String]) to [OutputStream].
 * The byte order can't be changed and is always little-endian.
 *
 * The class provides emit-like methods that write a value and move the cursor forward for the amount of bytes that was written.
 * Because of its stream nature, there's no going back and the cursor can't be moved forward or backward.
 *
 * The writer is already buffered, thus a reasonable buffer size should be specified in constructor.
 */
class PrimitiveValueWriter(private val output: OutputStream, bufferSize: Int) {
    private val byteBufferArray = ByteArray(bufferSize)

    // Stores a current position in byteBufferArray from which writing the data should start.
    // It can't equal to byteBufferArray.size and should be even.
    private var bufferPos = 0

    fun emit(b: Byte) {
        val buf = byteBufferArray
        var bp = bufferPos
        val remaining = buf.size - bp

        if (remaining == 0) {
            output.write(buf, 0, buf.size)
            bp = 0
        }

        buf[bp++] = b
        setBufferPosAndSyncIfNecessary(bp)
    }

    private fun emit2Bytes(b1: Byte, b2: Byte) {
        emitNBytes(
            length = 2,
            common = {
                emit(b1)
                emit(b2)
            },
            fast = { buf, bp ->
                buf[bp] = b1
                buf[bp + 1] = b2
            }
        )
    }

    private fun emit3Bytes(b1: Byte, b2: Byte, b3: Byte) {
        emitNBytes(
            length = 3,
            common = {
                emit(b1)
                emit(b2)
                emit(b3)
            },
            fast = { buf, bp ->
                buf[bp] = b1
                buf[bp + 1] = b2
                buf[bp + 2] = b3
            }
        )
    }

    private fun emit4Bytes(b1: Byte, b2: Byte, b3: Byte, b4: Byte) {
        emitNBytes(
            length = 4,
            common = {
                emit(b1)
                emit(b2)
                emit(b3)
                emit(b4)
            },
            fast = { buf, bp ->
                buf[bp] = b1
                buf[bp + 1] = b2
                buf[bp + 2] = b3
                buf[bp + 3] = b4
            }
        )
    }

    private inline fun emitNBytes(length: Int, common: () -> Unit, fast: (buf: ByteArray, bp: Int) -> Unit) {
        val buf = byteBufferArray
        val bp = bufferPos
        val remaining = buf.size - bp

        if (remaining < length) {
            common()
        } else {
            fast(buf, bp)

            setBufferPosAndSyncIfNecessary(bp + length)
        }
    }

    fun emit(c: Char) = emit(c.code.toShort())

    fun emit(value: Short) {
        emitNumberPrimitive(value.toLong(), byteCount = 2)
    }

    fun emit(value: Int) {
        emitNumberPrimitive(value.toLong(), byteCount = 4)
    }

    fun emit(value: Long) {
        emitNumberPrimitive(value, byteCount = 8)
    }

    private fun emitNumberPrimitive(value: Long, byteCount: Int) {
        val buf = byteBufferArray
        var bp = bufferPos
        val remaining = buf.size - bp

        if (remaining >= byteCount) {
            writeLongPartToBuffer(buf, bp, value, 0, byteCount)
            bp += byteCount
        } else {
            writeLongPartToBuffer(buf, bp, value, 0, remaining)

            // If remaining < byteCount, it means that we can't write enough bits of value to the buffer and
            // it needs to be written to the stream first.
            output.write(buf, 0, buf.size)

            val remainingInValue = byteCount - remaining

            writeLongPartToBuffer(buf, 0, value, remaining, remainingInValue)
            bp = remainingInValue
        }

        setBufferPosAndSyncIfNecessary(bp)
    }

    fun emitUtf8(value: String) {
        val utf8Size = value.utf8Size()

        checkStringLength(utf8Size)
        emit(utf8Size.toShort())

        value.processUtf8Bytes(
            on1Byte = ::emit,
            on2Byte = ::emit2Bytes,
            on3Byte = ::emit3Bytes,
            on4Byte = ::emit4Bytes,
        )
    }

    fun emitUtf16(value: String) {
        val valueLength = value.length

        checkStringLength(valueLength)
        emit(valueLength.toShort())

        for (i in 0 until valueLength) {
            emit(value[i])
        }
    }

    fun emitString(value: String, isUtf8: Boolean) {
        if (isUtf8) {
            emitUtf8(value)
        } else {
            emitUtf16(value)
        }
    }

    fun emit(ints: IntArray, start: Int, end: Int) {
        emitPrimitiveArray(ints, start, end, IntArray::get, ::emit)
    }

    fun emitArrayAndLength(ints: IntArray, start: Int = 0, end: Int = ints.size) {
        val length = end - start

        emit(length)
        emit(ints, start, end)
    }

    private inline fun <TArray, TElement> emitPrimitiveArray(
        array: TArray,
        start: Int, end: Int,
        getElement: TArray.(index: Int) -> TElement,
        emitElement: (TElement) -> Unit
    ) {
        for (i in start until end) {
            emitElement(array.getElement(i))
        }
    }

    fun <T : Any> emit(
        values: Array<out T>,
        serializer: BinarySerializer<in T>,
        compatInfo: BinarySerializationCompatInfo,
        progressReporter: ProgressReporter? = null
    ) {
        val size = values.size
        emit(size)

        trackLoopProgressWith(progressReporter, size) { i ->
            serializer.writeTo(this, values[i], compatInfo)
        }
    }

    fun flush() {
        val bp = bufferPos
        if (bp > 0) {
            output.write(byteBufferArray, 0, bp)
            bufferPos = 0
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

        internal fun writeLongPartToBuffer(
            buffer: ByteArray,
            bufferStart: Int,
            value: Long,
            valueStart: Int,
            length: Int
        ) {
            var shift = valueStart shl 3

            for (i in 0 until length) {
                val b = (value shr shift).toByte()
                shift += 8

                buffer[bufferStart + i] = b
            }
        }
    }
}