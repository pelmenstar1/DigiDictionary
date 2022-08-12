package io.github.pelmenstar1.digiDict.common.serialization

import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.CharBuffer

class ValueWriter(private val buffer: ByteBuffer) {
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

    fun int32(value: Int) {
        buffer.putInt(value)
    }

    fun int64(value: Long) {
        buffer.putLong(value)
    }

    fun stringUtf16(value: String) {
        val valueLength = value.length
        checkStringLength(valueLength)

        buffer.also {
            it.putShort(valueLength.toShort())

            for (i in 0 until valueLength) {
                it.putChar(value[i])
            }
        }
    }

    fun stringUtf16(chars: CharArray, start: Int, end: Int) {
        val length = end - start
        checkStringLength(length)

        val bb = buffer
        val cb = charBuffer

        bb.putShort(length.toShort())
        val bbPos = bb.position()

        cb.position(bbPos / 2 - initialCharBufferPosition)
        cb.put(chars, start, length)

        bb.position(bbPos + length * 2)
    }

    companion object {
        internal fun checkStringLength(length: Int) {
            if (length > 65535) {
                throw IllegalArgumentException("Length of the string to be written can't be greater than 65535")
            }
        }
    }
}