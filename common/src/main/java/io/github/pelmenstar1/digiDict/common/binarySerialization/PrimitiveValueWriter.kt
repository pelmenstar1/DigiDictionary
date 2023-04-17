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
    private val buffer = ByteArray(bufferSize)

    // Stores a current position in buffer from which writing the data should start.
    // It shouldn't be equal to buffer.size, if it's, the buffer must be written to the OutputStream.
    private var bufferPos = 0

    fun emit(b: Byte) {
        val buf = buffer
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
        val buf = buffer
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
        emitNumberPrimitive(
            value,
            byteCount = 2,
            writeToBuffer = { buffer, offset ->
                buffer[offset] = value.toByte()
                buffer[offset + 1] = (value.toInt() shr 8).toByte()
            },
            toLong = Short::toLong
        )
    }

    fun emit(value: Int) {
        emitNumberPrimitive(
            value,
            byteCount = 4,
            writeToBuffer = { buffer, offset ->
                buffer[offset] = value.toByte()
                buffer[offset + 1] = (value shr 8).toByte()
                buffer[offset + 2] = (value shr 16).toByte()
                buffer[offset + 3] = (value shr 24).toByte()
            },
            toLong = Int::toLong
        )
    }

    fun emit(value: Long) {
        emitNumberPrimitive(
            value,
            byteCount = 8,
            writeToBuffer = { buffer, offset ->
                buffer[offset] = value.toByte()
                buffer[offset + 1] = (value shr 8).toByte()
                buffer[offset + 2] = (value shr 16).toByte()
                buffer[offset + 3] = (value shr 24).toByte()
                buffer[offset + 4] = (value shr 32).toByte()
                buffer[offset + 5] = (value shr 40).toByte()
                buffer[offset + 6] = (value shr 48).toByte()
                buffer[offset + 7] = (value shr 56).toByte()
            },
            toLong = { it }
        )
    }

    private inline fun <T> emitNumberPrimitive(
        value: T,
        byteCount: Int,
        writeToBuffer: (buffer: ByteArray, offset: Int) -> Unit,
        toLong: (T) -> Long
    ) {
        val buf = buffer
        var bp = bufferPos
        val remaining = buf.size - bp

        if (remaining >= byteCount) {
            writeToBuffer(buf, bp)
            bp += byteCount
        } else {
            val valueLong = toLong(value)

            // Write what we can to the buffer
            writeLongPartToBuffer(buf, bp, valueLong, 0, remaining)

            // Write buffer
            output.write(buf, 0, buf.size)

            val remainingInValue = byteCount - remaining

            // Write the remaining part to the buffer
            writeLongPartToBuffer(buf, 0, valueLong, remaining, remainingInValue)
            bp = remainingInValue
        }

        setBufferPosAndSyncIfNecessary(bp)
    }

    fun emitUtf8(value: String) {
        val utf8Size = value.utf8Size()

        checkStringLength(utf8Size)
        emit(utf8Size.toShort())

        if (bufferPos + utf8Size < buffer.size) {
            emitUtf8NoOverflow(value)
        } else {
            emitUtf8Overflow(value)
        }
    }

    private fun emitUtf8Overflow(value: String) {
        value.processUtf8Bytes(
            on1Byte = ::emit,
            on2Byte = ::emit2Bytes,
            on3Byte = ::emit3Bytes,
            on4Byte = ::emit4Bytes,
        )
    }

    private fun emitUtf8NoOverflow(value: String) {
        val buf = buffer
        var bp = bufferPos

        value.processUtf8Bytes(
            on1Byte = { b1 ->
                buf[bp++] = b1
            },
            on2Byte = { b1, b2 ->
                buf[bp] = b1
                buf[bp + 1] = b2

                bp += 2
            },
            on3Byte = { b1, b2, b3 ->
                buf[bp] = b1
                buf[bp + 1] = b2
                buf[bp + 2] = b3

                bp += 3
            },
            on4Byte = { b1, b2, b3, b4 ->
                buf[bp] = b1
                buf[bp + 1] = b2
                buf[bp + 2] = b3
                buf[bp + 3] = b4

                bp += 4
            }
        )

        setBufferPosAndSyncIfNecessary(bp)
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
            output.write(buffer, 0, bp)
            bufferPos = 0
        }
    }

    private fun setBufferPosAndSyncIfNecessary(newPos: Int) {
        val buf = buffer
        val bufSize = buf.size

        bufferPos = if (newPos == bufSize) {
            output.write(buf, 0, bufSize)

            0
        } else {
            newPos
        }
    }

    companion object {
        @JvmStatic
        internal fun checkStringLength(length: Int) {
            if (length > 65535) {
                throw IllegalArgumentException("Length of the string to be written can't be greater than 65535")
            }
        }

        @JvmStatic
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