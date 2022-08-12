package io.github.pelmenstar1.digiDict.common.serialization

import io.github.pelmenstar1.digiDict.common.ProgressReporter
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.channels.FileChannel
import java.nio.channels.WritableByteChannel

private fun WritableByteChannel.writeAllBuffer(buffer: ByteBuffer) {
    while (buffer.hasRemaining()) {
        write(buffer)
    }
}

private const val BUFFER_SIZE = 4096
private const val MAGIC_WORD = 0x00FF00FF_abcdedf00L

fun <T : Any> WritableByteChannel.writeValues(
    values: Array<out T>,
    serializer: BinarySerializer<in T>,
    progressReporter: ProgressReporter? = null
) {
    writeValues(SerializableIterable(values, serializer), progressReporter)
}

fun WritableByteChannel.writeValues(values: SerializableIterable, progressReporter: ProgressReporter? = null) {
    // FileChannel in Android always create wrapping direct buffer
    // if input buffer is not direct, so create it as a direct one in the first place.
    val buffer = ByteBuffer.allocateDirect(BUFFER_SIZE)
    val writer = ValueWriter.of(buffer)
    val valuesSize = values.size

    buffer.apply {
        order(ByteOrder.LITTLE_ENDIAN)

        putLong(MAGIC_WORD)
        putInt(values.version)
        putInt(valuesSize)
    }

    val iterator = values.iterator()
    var index = 0

    while (iterator.moveToNext()) {
        iterator.initCurrent()
        val byteSize = iterator.getCurrentElementByteSize()

        if (byteSize >= BUFFER_SIZE) {
            throw IllegalStateException("Illegal record (Too big)")
        }

        // Check if we can proceed writing to temporary buffer, if don't, write the buffer to the file.
        val bufferPos = buffer.position()
        if (bufferPos + byteSize > BUFFER_SIZE) {
            buffer.also {
                it.limit(bufferPos)
                it.position(0)
                writeAllBuffer(buffer)

                it.limit(BUFFER_SIZE)
                it.position(0)
            }
        }

        iterator.writeCurrentElement(writer)

        progressReporter?.onProgress(index, valuesSize)
        index++
    }

    if (buffer.hasRemaining()) {
        buffer.flip()
        writeAllBuffer(buffer)
    }

    progressReporter?.onEnd()
}

fun <T : Any> FileChannel.readValuesToArray(
    serializerResolver: BinarySerializerResolver<T>,
    progressReporter: ProgressReporter? = null
): Array<T> {
    val buffer = map(FileChannel.MapMode.READ_ONLY, 0, size()).also {
        it.order(ByteOrder.LITTLE_ENDIAN)
    }

    val magicWord = buffer.long
    if (magicWord != MAGIC_WORD) {
        throw ValidationException("Magic words are wrong")
    }

    val version = buffer.int
    val serializer = serializerResolver.getOrLatest(version)

    return ValueReader.of(buffer).array(serializer, progressReporter)
}