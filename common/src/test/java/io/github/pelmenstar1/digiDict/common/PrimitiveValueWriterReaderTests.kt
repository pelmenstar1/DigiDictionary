package io.github.pelmenstar1.digiDict.common

import io.github.pelmenstar1.digiDict.common.binarySerialization.PrimitiveValueReader
import io.github.pelmenstar1.digiDict.common.binarySerialization.PrimitiveValueWriter
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import kotlin.test.assertEquals

class PrimitiveValueWriterReaderTests {
    private val bigString1 = "1".repeat(1024)
    private val bigString2 = (0..1024).joinToString()

    @Test
    fun readWriteTest() {
        val output = ByteArrayOutputStream()
        val writer = PrimitiveValueWriter(output)

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
            stringUtf16(bigString1)
            stringUtf16(bigString2)
        }

        val reader = PrimitiveValueReader(ByteArrayInputStream(output.toByteArray()))

        assertEquals(1, reader.int32())
        assertEquals(-100, reader.int32())
        assertEquals(383, reader.int64())
        assertEquals(222, reader.int32())
        assertEquals("123", reader.stringUtf16())
        assertEquals(0, reader.int32())
        assertEquals("55555", reader.stringUtf16())
        assertEquals("", reader.stringUtf16())
        assertEquals("1", reader.stringUtf16())
        assertEquals(bigString1, reader.stringUtf16())
        assertEquals(bigString2, reader.stringUtf16())
    }
}