package io.github.pelmenstar1.digiDict.common.binarySerialization

import io.github.pelmenstar1.digiDict.common.ProgressReporter
import io.github.pelmenstar1.digiDict.common.trackLoopProgressWith
import java.io.OutputStream
import kotlin.math.min

class PrimitiveValueWriter(private val output: OutputStream) {
    // Temporary buffer for writing primitive values to and for caching chunk of string's data.
    private val internalBuffer = ByteArray(INTERNAL_BUFFER_SIZE)

    private fun writeInt16Internal(value: Int, offset: Int, buffer: ByteArray) {
        buffer[offset] = value.toByte()
        buffer[offset + 1] = (value shr 8).toByte()
    }

    fun int16(value: Int) {
        writePrimitive(byteCount = 2) { writeInt16Internal(value, offset = 0, buffer = it) }
    }

    fun int32(value: Int) {
        writePrimitive(byteCount = 4) {
            it[0] = value.toByte()
            it[1] = (value shr 8).toByte()
            it[2] = (value shr 16).toByte()
            it[3] = (value shr 24).toByte()
        }
    }

    fun int64(value: Long) {
        writePrimitive(byteCount = 8) {
            it[0] = value.toByte()
            it[1] = (value shr 8).toByte()
            it[2] = (value shr 16).toByte()
            it[3] = (value shr 24).toByte()
            it[4] = (value shr 32).toByte()
            it[5] = (value shr 40).toByte()
            it[6] = (value shr 48).toByte()
            it[7] = (value shr 56).toByte()
        }
    }

    private inline fun writePrimitive(byteCount: Int, writeToBuffer: (ByteArray) -> Unit) {
        val buffer = internalBuffer
        writeToBuffer(buffer)

        output.write(buffer, 0, byteCount)
    }

    fun stringUtf16(value: String) {
        val valueLength = value.length
        val buffer = internalBuffer

        checkStringLength(valueLength)

        // checkStringLength() checks whether a string length bigger than int16's maximum.
        int16(valueLength)

        var strOffset = 0
        var bufferOffset = 0

        while (strOffset < valueLength) {
            val end = min(strOffset + INTERNAL_BUFFER_SIZE / 2, valueLength)

            for (i in strOffset until end) {
                writeInt16Internal(value[i].code, bufferOffset, buffer)
                bufferOffset += 2
            }

            output.write(buffer, 0, bufferOffset)

            bufferOffset = 0
            strOffset = end
        }
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

    companion object {
        private const val INTERNAL_BUFFER_SIZE = 32

        internal fun checkStringLength(length: Int) {
            if (length > 65535) {
                throw IllegalArgumentException("Length of the string to be written can't be greater than 65535")
            }
        }
    }
}