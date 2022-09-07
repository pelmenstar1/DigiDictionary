package io.github.pelmenstar1.digiDict.common

import io.github.pelmenstar1.digiDict.common.binarySerialization.PrimitiveValueReader
import io.github.pelmenstar1.digiDict.common.binarySerialization.PrimitiveValueWriter
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import kotlin.test.assertEquals

class PrimitiveValueWriterReaderTests {
    private sealed class Operation<T>(val value: T)
    private class ShortOperation(value: Short) : Operation<Short>(value)
    private class IntOperation(value: Int) : Operation<Int>(value)
    private class LongOperation(value: Long) : Operation<Long>(value)
    private class StringOperation(value: String) : Operation<String>(value)

    private class OperationChainBuilder(private val ops: MutableList<Operation<out Any>>) {
        fun short(value: Short) = add(ShortOperation(value))
        fun int(value: Int) = add(IntOperation(value))
        fun long(value: Long) = add(LongOperation(value))
        fun string(value: String) = add(StringOperation(value))

        private fun add(op: Operation<out Any>) {
            ops.add(op)
        }
    }

    private val bigString1 = "1".repeat(1024)
    private val bigString2 = (0..1024).joinToString()

    private val defaultBufferSizes = intArrayOf(32, 64, 128, 1024, 4096)

    private fun readWriteTestHelper(bufferSize: Int, block: OperationChainBuilder.() -> Unit) {
        readWriteTestHelper(intArrayOf(bufferSize), block)
    }

    private fun readWriteTestHelper(bufSizes: IntArray = defaultBufferSizes, block: OperationChainBuilder.() -> Unit) {
        val ops = ArrayList<Operation<out Any>>()
        OperationChainBuilder(ops).block()

        for (bufSize in bufSizes) {
            val output = ByteArrayOutputStream()
            val writer = PrimitiveValueWriter(output, bufSize)

            for (op in ops) {
                writer.run {
                    when (op) {
                        is ShortOperation -> emit(op.value)
                        is IntOperation -> emit(op.value)
                        is LongOperation -> emit(op.value)
                        is StringOperation -> emit(op.value)
                    }
                }
            }
            writer.flush()

            val reader = PrimitiveValueReader(ByteArrayInputStream(output.toByteArray()), bufSize)

            for (op in ops) {
                val actualValue = reader.run {
                    when (op) {
                        is ShortOperation -> consumeShort()
                        is IntOperation -> consumeInt()
                        is LongOperation -> consumeLong()
                        is StringOperation -> consumeStringUtf16()
                    }
                }

                assertEquals(op.value, actualValue, "bufferSize: $bufSize")
            }
        }
    }

    @Test
    fun readWriteTest_mixed1() = readWriteTestHelper {
        short(1245)
        int(1)
        int(-100)
        int(383)
        long(222L)
        string("123")
        int(0)
        string("55555")
        string("")
        string("1")
        string(bigString1)
        string(bigString2)
    }

    @Test
    fun readWriteTest_mixed2() = readWriteTestHelper {
        long(1L)
        int(-100)
        int(383)
        long(222L)
        string("123")
        string("44445")
        int(0)
        string("55555")
        string("")
        string("1")
        string(bigString1)
        string(bigString2)
        int(344)
        string("123")
        long(1L)
        long(0L)
        int(Int.MAX_VALUE)
        long(Long.MAX_VALUE)
        int(Int.MIN_VALUE)
        long(Long.MIN_VALUE)
        short(Short.MAX_VALUE)
        int(12)
        short(Short.MIN_VALUE)
    }

    @Test
    fun readWriteNearBufferLimit() {
        readWriteTestHelper(bufferSize = 32) {
            // Make string to fulfill the buffer: 2 bytes for length, 2 bytes for each char => 2 + 15 * 2 = 32
            string(" ".repeat(15))

            // Then write other data
            int(123)
        }

        readWriteTestHelper(bufferSize = 32) {
            long(8L)
            long(Long.MAX_VALUE)
            long(Long.MIN_VALUE)
            long(0L)

            // We've written 4 longs (8 * 4 = 32), then write a string.
            string("123")
        }
    }

    @Test
    fun readWriteCrossBuffer() {
        readWriteTestHelper(bufferSize = 32) {
            // Make string to almost reach the end of the buffer: 2 + 14 * 2 = 30
            string(" ".repeat(14))

            // Make cross-buffer write (low bits in current buffer, high bits in the same buffer but at another position)
            int(12)
        }

        readWriteTestHelper(bufferSize = 32) {
            string(" ".repeat(14))

            long(Long.MAX_VALUE)
        }
    }

    @Test
    fun readWriteString_1() {
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
    fun readWriteString_2() {
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

    @Test
    fun readWriteString_mixed() {
        readWriteTestHelper {
            string(bigString2)
            string("")
            string("1")
            string("111111")
            string(bigString1)
            string("33")
        }
    }
}