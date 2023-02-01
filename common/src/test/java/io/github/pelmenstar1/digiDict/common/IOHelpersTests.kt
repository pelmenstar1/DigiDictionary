package io.github.pelmenstar1.digiDict.common

import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.InputStream
import kotlin.test.assertEquals
import kotlin.test.assertFails

class IOHelpersTests {
    private class ChunkedSequenceInputStream(private val size: Int, private val chunkSize: Int) : InputStream() {
        private var index = 0

        override fun read(): Int {
            if (index == size) {
                return -1
            }

            return (index++).toByte().toInt()
        }

        override fun read(b: ByteArray, off: Int, len: Int): Int {
            if (index >= size) {
                return -1
            }

            val resolvedSize = minOf(chunkSize, len, size - index)

            for (i in 0 until resolvedSize) {
                b[off + i] = (index++).toByte()
            }

            return resolvedSize
        }
    }

    private fun assertBufferContentValid(buffer: ByteArray, offset: Int) {
        for (i in 0 until (buffer.size - offset)) {
            assertEquals(i.toByte().toInt(), buffer[i + offset].toInt())
        }
    }

    @Test
    fun readAtLeastThrowsWhenMinLengthIsNegative() {
        val stream = ByteArrayInputStream(ByteArray(0))

        assertFails {
            stream.readAtLeast(ByteArray(0), offset = 0, minLength = -1, maxLength = 5)
        }
    }

    @Test
    fun readAtLeastThrowsWhenMaxLengthIsNegative() {
        val stream = ByteArrayInputStream(ByteArray(0))

        assertFails {
            stream.readAtLeast(ByteArray(0), offset = 0, minLength = 5, maxLength = -1)
        }
    }

    @Test
    fun readAtLeastThrowsWhenMinLengthIsGreaterThanMaxLength() {
        val stream = ByteArrayInputStream(ByteArray(0))

        assertFails {
            stream.readAtLeast(ByteArray(0), offset = 0, minLength = 5, maxLength = 1)
        }
    }

    private fun readAtLeastTestHelper(
        streamSize: Int, chunkSize: Int,
        minLength: Int, maxLength: Int,
        expectedReadBytes: Int
    ) {
        val stream = ChunkedSequenceInputStream(streamSize, chunkSize)

        val buffer = ByteArray(1 + expectedReadBytes)
        // Just a random number to check whether readAtLeast does mutate the data it shouldn't
        buffer[0] = 12

        val readBytes = stream.readAtLeast(buffer, offset = 1, minLength, maxLength)
        assertEquals(expectedReadBytes, readBytes)

        assertEquals(12, buffer[0])
        assertBufferContentValid(buffer, offset = 1)
    }

    @Test
    fun readAtLeastTest() {
        readAtLeastTestHelper(
            streamSize = 16, chunkSize = 16,
            minLength = 8, maxLength = 16,
            expectedReadBytes = 16
        )

        readAtLeastTestHelper(
            streamSize = 16, chunkSize = 16,
            minLength = 8, maxLength = 32,
            expectedReadBytes = 16
        )

        readAtLeastTestHelper(
            streamSize = 16, chunkSize = 16,
            minLength = 16, maxLength = 16,
            expectedReadBytes = 16
        )

        readAtLeastTestHelper(
            streamSize = 128, chunkSize = 16,
            minLength = 128, maxLength = 128,
            expectedReadBytes = 128
        )

        readAtLeastTestHelper(
            streamSize = 128, chunkSize = 16,
            minLength = 32, maxLength = 64,
            expectedReadBytes = 64
        )

        readAtLeastTestHelper(
            streamSize = 128, chunkSize = 16,
            minLength = 8, maxLength = 16,
            expectedReadBytes = 16
        )

        readAtLeastTestHelper(
            streamSize = 128, chunkSize = 16,
            minLength = 128, maxLength = 512,
            expectedReadBytes = 128
        )
    }

    @Test
    fun readAtLeastThrowsWhenNotEnoughDataTest() {
        assertFails {
            val stream = ChunkedSequenceInputStream(size = 16, chunkSize = 16)

            val buffer = ByteArray(17)
            stream.readAtLeast(buffer, offset = 1, minLength = 32, maxLength = 32)
        }
    }
}