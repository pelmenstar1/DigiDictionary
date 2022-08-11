package io.github.pelmenstar1.digiDict.common.serialization

import io.github.pelmenstar1.digiDict.common.ProgressReporter
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.channels.FileChannel
import java.nio.channels.WritableByteChannel

private inline fun ioOperation(method: () -> Int) {
    while (true) {
        val n = method()
        if (n <= 0) {
            break
        }
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
    // if input buffer is not direct, so create it as a direct in the first place.
    val buffer = ByteBuffer.allocateDirect(BUFFER_SIZE)
    val writer = ValueWriter.of(buffer)
    val valuesSize = values.size

    buffer.order(ByteOrder.LITTLE_ENDIAN)

    buffer.putLong(MAGIC_WORD)
    buffer.putInt(values.version)
    buffer.putInt(valuesSize)

    val iterator = values.iterator()
    var index = 0

    while (true) {
        // If there's no elements next, write current buffer.
        if (!iterator.moveToNext()) {
            buffer.also {
                it.limit(it.position())
                it.position(0)
                ioOperation { write(buffer) }
            }

            break
        }

        iterator.initCurrent()
        val byteSize = iterator.getCurrentElementByteSize()

        if (byteSize >= BUFFER_SIZE) {
            throw IllegalStateException("Illegal record (Too big)")
        }

        // Check if we can proceed writing to temporary buffer, if not, write it to the file.
        val bufferPos = buffer.position()
        if (bufferPos + byteSize > BUFFER_SIZE) {
            buffer.also {
                it.limit(bufferPos)
                it.position(0)
                ioOperation { write(it) }
                it.limit(BUFFER_SIZE)
                it.position(0)
            }

            iterator.writeCurrentElement(writer)
        } else {
            iterator.writeCurrentElement(writer)
        }

        progressReporter?.onProgress(index, valuesSize)
        index++
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