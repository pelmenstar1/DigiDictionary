package io.github.pelmenstar1.digiDict.common.serialization

import io.github.pelmenstar1.digiDict.common.ProgressReporter
import io.github.pelmenstar1.digiDict.common.nextPowerOf2
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.channels.FileChannel
import java.nio.channels.WritableByteChannel

private fun WritableByteChannel.writeAllFlippedBuffer(buffer: ByteBuffer) {
    buffer.flip()

    while (buffer.hasRemaining()) {
        write(buffer)
    }
}

private const val INITIAL_BUFFER_SIZE = 4096
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
    var buffer = ByteBuffer.allocateDirect(INITIAL_BUFFER_SIZE)
    var writer = ValueWriter(buffer)
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

        // If byte-size of single value is bigger than whole buffer capacity, it means
        // there's no way iterator.writeCurrentElement can write the content to the buffer without
        // overflow. It can happen extremely rarely. If that's the case, we write what we have
        // and recreate the buffer to make it possible to contain the current value.
        if (byteSize >= buffer.capacity()) {
            writeAllFlippedBuffer(buffer)

            // Presumably, it's faster to allocate a buffer when the capacity is power of 2.
            buffer = ByteBuffer.allocateDirect(byteSize.nextPowerOf2())

            // Update writer too, it holds old buffer.
            writer = ValueWriter(buffer)
        }

        val bufCapacity = buffer.capacity()

        // If the value can't be written to the current position of buffer, write the buffer to the file
        // and reset buffer's position.
        if (buffer.position() + byteSize > bufCapacity) {
            buffer.also {
                writeAllFlippedBuffer(it)

                it.limit(bufCapacity)
                it.position(0)
            }
        }

        iterator.writeCurrentElement(writer)

        progressReporter?.onProgress(index, valuesSize)
        index++
    }

    writeAllFlippedBuffer(buffer)

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

    return ValueReader(buffer).array(serializer, progressReporter)
}