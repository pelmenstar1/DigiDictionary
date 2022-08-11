package io.github.pelmenstar1.digiDict.common

import io.github.pelmenstar1.digiDict.common.serialization.BinarySize
import io.github.pelmenstar1.digiDict.common.serialization.ValueReader
import io.github.pelmenstar1.digiDict.common.serialization.ValueWriter
import org.junit.Test
import java.nio.ByteBuffer
import kotlin.test.assertEquals

class BinarySerializingTests {
    private val capacity = with(BinarySize) {
        int32 + int32 + int64 + int32 + int32 +
                stringUtf16("123") +
                stringUtf16("55555") +
                stringUtf16("") +
                stringUtf16("1") +
                stringUtf16("123")
    }

    private fun readWriteTestInternal(
        createWriter: () -> ValueWriter,
        createReader: () -> ValueReader
    ) {
        val writer = createWriter()

        writer.run {
            int32(1)
            int32(-100)
            int64(383)
            int32(222)
            stringUtf16("123")
            int32(0)
            stringUtf16("55555")
            stringUtf16("")
            stringUtf16("1")
            stringUtf16(charArrayOf(' ', '1', '2', '3', ' '), 1, 4)
        }

        val reader = createReader()

        assertEquals(1, reader.int32())
        assertEquals(-100, reader.int32())
        assertEquals(383, reader.int64())
        assertEquals(222, reader.int32())
        assertEquals("123", reader.stringUtf16())
        assertEquals(0, reader.int32())
        assertEquals("55555", reader.stringUtf16())
        assertEquals("", reader.stringUtf16())
        assertEquals("1", reader.stringUtf16())
        assertEquals("123", reader.stringUtf16())
    }

    @Test
    fun `read-write to byte array`() {
        val buffer = ByteArray(capacity)

        readWriteTestInternal(
            { ValueWriter.of(buffer) },
            { ValueReader.of(buffer) }
        )
    }

    @Test
    fun `read-write to byte buffer`() {
        val buffer = ByteBuffer.allocate(capacity)

        readWriteTestInternal(
            { ValueWriter.of(buffer) },
            {
                buffer.position(0)
                ValueReader.of(buffer)
            }
        )
    }
}