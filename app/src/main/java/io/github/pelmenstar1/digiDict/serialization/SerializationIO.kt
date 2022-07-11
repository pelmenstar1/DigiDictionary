package io.github.pelmenstar1.digiDict.serialization

import io.github.pelmenstar1.digiDict.validate
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

private const val BUFFER_SIZE = 2048
private const val MAGIC_WORD = 0x00FF00FF_abcdedf00L

fun WritableByteChannel.writeValues(values: SerializableIterable) {
    // FileChannel in Android always create wrapping direct buffer
    // if input buffer is not direct, so create it as a direct in the first place.
    val buffer = ByteBuffer.allocateDirect(BUFFER_SIZE)
    val writer = ValueWriter.of(buffer)

    buffer.order(ByteOrder.LITTLE_ENDIAN)

    buffer.putLong(MAGIC_WORD)
    buffer.putInt(values.size)

    val iterator = values.iterator()

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
    }
}

fun <T : Any> FileChannel.readValues(
    serializer: BinarySerializer<T>
): Sequence<T> {
    val buffer = map(FileChannel.MapMode.READ_ONLY, 0, size()).also {
        it.order(ByteOrder.LITTLE_ENDIAN)
    }

    val magicWord = buffer.long
    validate(magicWord == MAGIC_WORD, "Magic words are wrong")

    return ValueReader.of(buffer).sequence(serializer)
}
