package io.github.pelmenstar1.digiDict.common

import io.github.pelmenstar1.digiDict.common.binarySerialization.PrimitiveValueReader
import io.github.pelmenstar1.digiDict.common.binarySerialization.PrimitiveValueWriter
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals

class PrimitiveValueWriterReaderTests {
    private sealed class Operation<T>(val value: T)
    private class ShortOperation(value: Short) : Operation<Short>(value)
    private class IntOperation(value: Int) : Operation<Int>(value)
    private class LongOperation(value: Long) : Operation<Long>(value)
    private class StringOperation(value: String, val isUtf8: Boolean) : Operation<String>(value)
    private class IntArrayOperation(value: IntArray) : Operation<IntArray>(value)

    private class OperationChainBuilder(private val ops: MutableList<Operation<out Any>>) {
        fun short(value: Short) = add(ShortOperation(value))
        fun int(value: Int) = add(IntOperation(value))
        fun long(value: Long) = add(LongOperation(value))
        fun string(value: String, isUtf8: Boolean) = add(StringOperation(value, isUtf8))
        fun intArray(value: IntArray) = add(IntArrayOperation(value))

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
                        is StringOperation -> if (op.isUtf8) emitUtf8(op.value) else emitUtf16(op.value)
                        is IntArrayOperation -> emitArrayAndLength(op.value)
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
                        is StringOperation -> if (op.isUtf8) consumeStringUtf8() else consumeStringUtf16()
                        is IntArrayOperation -> consumeIntArrayAndLength()
                    }
                }

                val msg = "bufferSize: $bufSize"
                val expectedValue = op.value

                if (actualValue is IntArray && expectedValue is IntArray) {
                    assertContentEquals(expectedValue, actualValue, msg)
                } else {
                    assertEquals(expectedValue, actualValue, msg)
                }

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
        string("123", isUtf8 = false)
        string("abcdef", isUtf8 = true)
        string("блаблабла", isUtf8 = true)  // Non-ASCII text
        int(0)
        string("55555", isUtf8 = true)
        string("", isUtf8 = true)
        string("1", isUtf8 = false)
        string(bigString1, isUtf8 = true)
        string(bigString2, isUtf8 = false)
        string(bigString2, isUtf8 = true)
        intArray(intArrayOf(0, 1, 2, 3))
        intArray(intArrayOf(Int.MAX_VALUE, 245, 444, 8))
        intArray(IntArray(1024) { it })
    }

    @Test
    fun readWriteTest_mixed2() = readWriteTestHelper {
        long(1L)
        int(-100)
        int(383)
        long(222L)
        string("123", isUtf8 = true)
        string("44445", isUtf8 = true)
        int(0)
        string("55555", isUtf8 = true)
        string("", isUtf8 = true)
        string("1", isUtf8 = true)
        string(bigString1, isUtf8 = true)
        string(bigString2, isUtf8 = true)
        int(344)
        string("123", isUtf8 = true)
        long(1L)
        intArray(intArrayOf(Int.MAX_VALUE, 12, 3))
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
            string(" ".repeat(15), isUtf8 = false)

            // Then write other data
            int(123)
        }

        readWriteTestHelper(bufferSize = 32) {
            // Make string to fulfill the buffer: 2 bytes for length, 2 bytes for each char => 2 + 15 * 2 = 32
            // Also, non-ASCII text.
            string("м".repeat(15), isUtf8 = true)

            // Then write other data
            int(123)
        }

        readWriteTestHelper(bufferSize = 32) {
            long(8L)
            long(Long.MAX_VALUE)
            long(Long.MIN_VALUE)
            long(0L)

            // We've written 4 longs (8 * 4 = 32), then write a string.
            string("123", isUtf8 = false)
        }

        readWriteTestHelper(bufferSize = 32) {
            long(8L)
            long(Long.MAX_VALUE)
            long(Long.MIN_VALUE)
            long(0L)

            // We've written 4 longs (8 * 4 = 32), then write a string.
            string("123", isUtf8 = true)
        }

        readWriteTestHelper(bufferSize = 32) {
            string(" ".repeat(15), isUtf8 = false)

            intArray(intArrayOf(1, 2, 3, 4))
        }

        readWriteTestHelper(bufferSize = 32) {
            string(" ".repeat(31), isUtf8 = true)

            intArray(intArrayOf(1, 2, 3, 4))
        }
    }

    @Test
    fun readWriteCrossBuffer_int() {
        readWriteTestHelper(bufferSize = 32) {
            // Make string to almost reach the end of the buffer: 2 + 14 * 2 = 30
            string(" ".repeat(14), isUtf8 = false)

            // Make cross-buffer write (low bits in current buffer, high bits in the same buffer but at another position)
            int(12)
        }
    }

    @Test
    fun readWriteCrossBuffer_long() {
        readWriteTestHelper(bufferSize = 32) {
            string(" ".repeat(14), isUtf8 = false)

            long(Long.MAX_VALUE)
        }
    }

    @Test
    fun readWriteCrossBuffer_string() {
        readWriteTestHelper(bufferSize = 32) {
            string(" ".repeat(14), isUtf8 = false)
            string("1111", isUtf8 = false)
        }

        readWriteTestHelper(bufferSize = 32) {
            string(" ".repeat(30), isUtf8 = true)
            string("1111", isUtf8 = true)
        }
    }

    @Test
    fun readWriteCrossBuffer_intArray_aligned() {
        readWriteTestHelper(bufferSize = 128) {
            intArray(IntArray(1024) { it })
        }
    }

    @Test
    fun readWriteCrossBuffer_intArray_nonAligned() {
        readWriteTestHelper(bufferSize = 128) {
            short(0)
            intArray(IntArray(1024) { it })
        }
    }

    @Test
    fun readWriteString_1_utf16() {
        readWriteTestHelper(bufferSize = 128) {
            string("123", isUtf8 = false)
            string("", isUtf8 = false)
            string("55555", isUtf8 = false)
        }
    }

    @Test
    fun readWriteString_smallStrings_utf8() {
        readWriteTestHelper(bufferSize = 128) {
            string("123", isUtf8 = true)
            string("", isUtf8 = true)
            string("55555", isUtf8 = true)
        }
    }

    @Test
    fun readWriteString_bigStrings_utf8() {
        readWriteTestHelper(bufferSize = 128) {
            string(bigString1, isUtf8 = true)
            string(bigString2, isUtf8 = true)
        }
    }

    @Test
    fun readWriteString_bigStrings_utf16() {
        readWriteTestHelper(bufferSize = 128) {
            string(bigString1, isUtf8 = false)
            string(bigString2, isUtf8 = false)
        }
    }

    @Test
    fun readWriteString_mixed_utf8() {
        readWriteTestHelper {
            string(bigString2, isUtf8 = true)
            string("", isUtf8 = true)
            string("1", isUtf8 = true)
            string("111111", isUtf8 = true)
            string(bigString1, isUtf8 = true)
            string("33", isUtf8 = true)
        }
    }

    @Test
    fun readWriteString_mixed_utf16() {
        readWriteTestHelper {
            string(bigString2, isUtf8 = false)
            string("", isUtf8 = false)
            string("1", isUtf8 = true)
            string("111111", isUtf8 = false)
            string(bigString1, isUtf8 = false)
            string("33", isUtf8 = false)
        }
    }
}