package io.github.pelmenstar1.digiDict.common.binarySerialization

import io.github.pelmenstar1.digiDict.common.ProgressReporter
import io.github.pelmenstar1.digiDict.common.getByteAt
import io.github.pelmenstar1.digiDict.common.trackLoopProgressWith
import java.io.OutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.CharBuffer
import kotlin.math.min

class PrimitiveValueWriter(private val output: OutputStream, bufferSize: Int) {
    private val byteBufferArray = ByteArray(bufferSize)
    private val byteBuffer: ByteBuffer
    private val byteBufferAsChar: CharBuffer

    private var charBuffer: CharArray? = null

    private var bufferPos = 0

    init {
        if (bufferSize % 2 != 0) {
            throw IllegalArgumentException("Size of buffer should be even")
        }

        byteBuffer = ByteBuffer.wrap(byteBufferArray).also {
            it.order(ByteOrder.LITTLE_ENDIAN)
        }

        byteBufferAsChar = byteBuffer.asCharBuffer()
    }

    fun int16(value: Int) {
        writePrimitive(value, byteCount = 2, Int::getByteAt)
    }

    fun int32(value: Int) {
        writePrimitive(value, byteCount = 4, Int::getByteAt)
    }

    fun int64(value: Long) {
        writePrimitive(value, byteCount = 8, Long::getByteAt)
    }

    private inline fun <T> writePrimitive(value: T, byteCount: Int, getByteAt: T.(offset: Int) -> Byte) {
        val buf = byteBufferArray
        var bp = bufferPos
        val remaining = buf.size - bp

        val minLength = min(byteCount, remaining)
        for (i in 0 until minLength) {
            buf[bp++] = value.getByteAt(i)
        }

        if (minLength != byteCount) {
            // If minLength != byteCount, it means that we can't write enough bits of value to the buffer and
            // it needs to be written to the stream first.
            output.write(buf, 0, bp)

            // After buffer synchronization, buffer position should be set to zero in order to continue writing of remaining
            // bits of value.
            bp = 0
            for (i in remaining until byteCount) {
                buf[bp++] = value.getByteAt(i)
            }
        }

        bufferPos = bp
    }

    fun stringUtf16(value: String) {
        var cb = charBuffer
        val valueLength = value.length

        checkStringLength(valueLength)
        int16(valueLength)

        if (valueLength > 0) {
            if (cb == null || valueLength > cb.size) {
                cb = CharArray(valueLength)
                charBuffer = cb
            }

            value.toCharArray(cb)
            stringUtf16(cb, 0, valueLength)
        }
    }

    fun stringUtf16(chars: CharArray, start: Int, end: Int) {
        val out = output
        val bb = byteBufferArray
        val bbAsChar = byteBufferAsChar
        var bp = bufferPos
        val bufSize = bb.size
        val bufSizeAsChar = bufSize / 2

        val charLength = end - start

        bbAsChar.position(bp / 2)
        val remCharsInByteBuffer = (bufSize - bp) / 2

        if (remCharsInByteBuffer > charLength) {
            // We can write a whole region of chars.
            bbAsChar.put(chars, start, charLength)
            bp += charLength * 2
        } else {
            var charPos = start

            // Write prefix of chars to fulfill the buffer and write it.
            bbAsChar.put(chars, charPos, remCharsInByteBuffer)

            charPos += remCharsInByteBuffer
            out.write(bb, 0, bufSize)

            // Stores amount of chars in 'chars' array that need to be written.
            var remChars: Int

            // This loop does full buffer writes so buffer position is considered to be 0.
            while (true) {
                remChars = end - charPos

                // Exit the loop if we can't do full buffer write.
                if (remChars < bufSizeAsChar) {
                    break
                }

                bbAsChar.position(0)
                bbAsChar.put(chars, charPos, bufSizeAsChar)

                charPos += bufSizeAsChar
                out.write(bb, 0, bufSize)
            }

            // Write suffix if it exists.
            if (remChars != 0) {
                bbAsChar.position(0)
                bbAsChar.put(chars, charPos, remChars)

                bp = remChars * 2
            }
        }

        bufferPos = bp
    }

    fun <T : Any> array(
        values: Array<out T>,
        serializer: BinarySerializer<in T>,
        progressReporter: ProgressReporter? = null
    ) {
        val size = values.size
        int32(size)

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

    companion object {
        internal fun checkStringLength(length: Int) {
            if (length > 65535) {
                throw IllegalArgumentException("Length of the string to be written can't be greater than 65535")
            }
        }
    }
}