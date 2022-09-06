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
    fun readWriteTest_mixed() {
        val output = ByteArrayOutputStream()
        val writer = PrimitiveValueWriter(output, bufferSize = 32)

        writer.run {
            emit(1)
            emit(-100)
            emit(383)
            emit(222)
            emit("123")
            emit(0)
            emit("55555")
            emit("")
            emit("1")
            emit(bigString1)
            emit(bigString2)

            flush()
        }

        val reader = PrimitiveValueReader(ByteArrayInputStream(output.toByteArray()), 32)

        assertEquals(1, reader.consumeInt())
        assertEquals(-100, reader.consumeInt())
        assertEquals(383, reader.consumeLong())
        assertEquals(222, reader.consumeInt())
        assertEquals("123", reader.consumeStringUtf16())
        assertEquals(0, reader.consumeInt())
        assertEquals("55555", reader.consumeStringUtf16())
        assertEquals("", reader.consumeStringUtf16())
        assertEquals("1", reader.consumeStringUtf16())
        assertEquals(bigString1, reader.consumeStringUtf16())
        assertEquals(bigString2, reader.consumeStringUtf16())
    }

    @Test
    fun readWriteString_allInBuffer() {
        val output = ByteArrayOutputStream()
        val writer = PrimitiveValueWriter(output, bufferSize = 128)
        writer.run {
            emit("123")
            emit("")
            emit("55555")

            flush()
        }

        val reader = PrimitiveValueReader(ByteArrayInputStream(output.toByteArray()), 128)
        assertEquals("123", reader.consumeStringUtf16())
        assertEquals("", reader.consumeStringUtf16())
        assertEquals("55555", reader.consumeStringUtf16())
    }

    @Test
    fun readWriteString_big() {
        val output = ByteArrayOutputStream()
        val writer = PrimitiveValueWriter(output, bufferSize = 128)
        writer.run {
            emit(bigString1)
            emit(bigString2)

            flush()
        }

        val reader = PrimitiveValueReader(ByteArrayInputStream(output.toByteArray()), 128)
        assertEquals(bigString1, reader.consumeStringUtf16())
        assertEquals(bigString2, reader.consumeStringUtf16())
    }
}