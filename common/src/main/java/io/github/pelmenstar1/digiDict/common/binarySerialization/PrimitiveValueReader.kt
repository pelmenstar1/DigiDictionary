package io.github.pelmenstar1.digiDict.common.binarySerialization

import io.github.pelmenstar1.digiDict.common.*
import java.io.InputStream
import kotlin.math.min

class PrimitiveValueReader(private val inputStream: InputStream, bufferSize: Int) {
    private var byteBufferForPrimitives: ByteArray? = null

    private val byteBuffer = ByteArray(bufferSize)
    private var consumedByteLength = 0
    private var actualBufferLength = -1

    private var charBuffer: CharArray? = null

    fun consumeShort() = readPrimitive(byteCount = 2, ByteArray::readShort)
    fun consumeInt() = readPrimitive(byteCount = 4, ByteArray::readInt)
    fun consumeLong() = readPrimitive(byteCount = 8, ByteArray::readLong)

    private inline fun <T> readPrimitive(byteCount: Int, readValue: ByteArray.(offset: Int) -> T): T {
        val buf = consumePrimitiveAsByteArray(byteCount)

        return buf.readValue(0)
    }

    private fun consumePrimitiveAsByteArray(byteCount: Int): ByteArray {
        val bb = byteBuffer
        val input = inputStream
        var actualBufLength = actualBufferLength
        var consumedBytes = consumedByteLength

        var bbForPrimitives = byteBufferForPrimitives
        if (bbForPrimitives == null) {
            bbForPrimitives = ByteArray(MAX_PRIMITIVE_LENGTH)
            byteBufferForPrimitives = bbForPrimitives
        }

        if (actualBufLength < 0) {
            actualBufLength = input.readAtLeast(bb, 0, minLength = byteCount, maxLength = bb.size)

            System.arraycopy(bb, 0, bbForPrimitives, 0, byteCount)
            consumedBytes = byteCount
        } else {
            val remainingBytes = actualBufLength - consumedBytes
            val prefixLength = min(remainingBytes, byteCount)
            val suffixLength = byteCount - prefixLength

            System.arraycopy(bb, consumedBytes, bbForPrimitives, 0, prefixLength)

            if (suffixLength != 0) {
                actualBufLength = input.readAtLeast(bb, 0, minLength = suffixLength, maxLength = bb.size)
                System.arraycopy(bb, 0, bbForPrimitives, prefixLength, suffixLength)
                consumedBytes = suffixLength
            } else {
                consumedBytes += prefixLength
            }
        }

        consumedByteLength = consumedBytes
        actualBufferLength = actualBufLength

        return bbForPrimitives
    }

    fun consumeStringUtf16(): String {
        val charLength = consumeShort().toInt() and 0xFFFF
        if (charLength == 0) {
            return ""
        }

        var cb = charBuffer
        if (cb == null || charLength > cb.size) {
            cb = CharArray(charLength)
            charBuffer = cb
        }

        consumeStringUtf16(cb, 0, charLength)
        return String(cb, 0, charLength)
    }

    @Suppress("ReplaceArrayEqualityOpWithArraysEquals")
    fun consumeStringUtf16(chars: CharArray, start: Int, end: Int) {
        val charLength = end - start
        val byteLength = charLength * 2

        val bb = byteBuffer
        val bufSize = bb.size
        val bufSizeAsChar = bufSize / 2
        val input = inputStream
        var actualBufLength = actualBufferLength
        var consumedBytes = consumedByteLength

        var charPos = start
        if (actualBufLength < 0) {
            actualBufLength = input.readAtLeast(bb, 0, minLength = min(byteLength, bufSize), maxLength = bufSize)
        }

        val remCachedBytes = actualBufLength - consumedBytes

        val prefixByteLength = min(byteLength, remCachedBytes)
        val consumedBytesAndPrefix = consumedBytes + prefixByteLength

        convertByteBufferToChars(consumedBytes, consumedBytesAndPrefix, chars, start)
        charPos += prefixByteLength / 2
        consumedBytes = consumedBytesAndPrefix

        if (charPos != end) {
            var remChars: Int

            while (true) {
                remChars = end - charPos
                if (remChars < bufSizeAsChar) {
                    break
                }

                input.readExact(bb, 0, bufSize)
                convertByteBufferToChars(0, bufSize, chars, charPos)
                charPos += bufSizeAsChar
            }

            if (remChars > 0) {
                val remCharsAsByte = remChars * 2
                actualBufLength = input.readAtLeast(bb, 0, minLength = remCharsAsByte, maxLength = bufSize)
                convertByteBufferToChars(0, remCharsAsByte, chars, charPos)

                consumedBytes = remCharsAsByte
            }
        }

        consumedByteLength = consumedBytes
        actualBufferLength = actualBufLength
    }

    private fun convertByteBufferToChars(byteStart: Int, byteEnd: Int, chars: CharArray, charStart: Int) {
        val bb = byteBuffer
        var byteOffset = byteStart
        var charPos = charStart

        while (byteOffset < byteEnd) {
            chars[charPos] = bb.readShort(byteOffset).toInt().toChar()
            charPos++
            byteOffset += 2
        }
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

    companion object {
        private const val MAX_PRIMITIVE_LENGTH = 8
    }
}