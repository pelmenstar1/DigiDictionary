package io.github.pelmenstar1.digiDict.common

import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.pelmenstar1.digiDict.common.binarySerialization.BinarySerializationConstants
import io.github.pelmenstar1.digiDict.common.binarySerialization.BinarySerializationDecoder
import io.github.pelmenstar1.digiDict.common.binarySerialization.BinarySerializationException
import io.github.pelmenstar1.digiDict.common.binarySerialization.PrimitiveValueReader
import org.junit.Test
import org.junit.runner.RunWith
import java.io.ByteArrayInputStream
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.fail

@RunWith(AndroidJUnit4::class)
class BinarySerializationDecoderTests {
    private fun assertFailsWithBinarySerializationException(reason: Int, block: () -> Unit) {
        try {
            block()
            fail("No BinarySerializationException was thrown")
        } catch (e: Exception) {
            if (e !is BinarySerializationException) {
                fail("The exception was expected to be BinarySerializationException", e)
            }

            assertEquals(reason, e.reason, "Mismatched reason")
        }
    }

    @Test
    fun decodeThrowsOnInvalidMagicWordTest() {
        val inputArray = ByteArray(8) // 0 is not a magic word
        val inputStream = ByteArrayInputStream(inputArray)
        val reader = PrimitiveValueReader(inputStream, bufferSize = 128)

        val decoder = BinarySerializationDecoder<BinarySerializationTestUtils.Keys>()

        assertFailsWithBinarySerializationException(BinarySerializationException.REASON_DATA_VALIDATION) {
            decoder.decode(reader, BinarySerializationTestUtils.staticInfo)
        }
    }

    @Test
    fun decodeThrowsOnUnknownVersionTest() {
        val inputArray = ByteArray(12).also { buffer ->
            BinarySerializationConstants.MAGIC_WORD.writeTo(buffer, offset = 0)
            123.writeTo(buffer, offset = 8) // 123 is invalid version of serializer
        }

        val inputStream = ByteArrayInputStream(inputArray)
        val reader = PrimitiveValueReader(inputStream, bufferSize = 128)

        val decoder = BinarySerializationDecoder<BinarySerializationTestUtils.Keys>()

        assertFailsWithBinarySerializationException(BinarySerializationException.REASON_UNKNOWN_VERSION) {
            decoder.decode(reader, BinarySerializationTestUtils.staticInfo)
        }
    }

    @Suppress("UNCHECKED_CAST")
    @Test
    fun decodeTest() {
        val inputArray = ByteArray(96).also { buffer ->
            BinarySerializationConstants.MAGIC_WORD.writeTo(buffer, offset = 0)

            1.writeTo(buffer, offset = 8) // latest version of serializer for Object1
            2.writeTo(buffer, offset = 12) // length of the first array

            0.writeTo(buffer, offset = 16) // id of the first value
            4.toShort().writeTo(buffer, offset = 20) // length of the data in the first value
            '1'.code.toShort().writeTo(buffer, offset = 22)
            '2'.code.toShort().writeTo(buffer, offset = 24)
            '3'.code.toShort().writeTo(buffer, offset = 26)
            '4'.code.toShort().writeTo(buffer, offset = 28)

            1.writeTo(buffer, offset = 30) // id of the second value
            3.toShort().writeTo(buffer, offset = 34) // length of the data in the second value
            '1'.code.toShort().writeTo(buffer, offset = 36)
            '2'.code.toShort().writeTo(buffer, offset = 38)
            '3'.code.toShort().writeTo(buffer, offset = 40)

            2.writeTo(buffer, offset = 42) // latest version of serializer for Object2
            2.writeTo(buffer, offset = 46) // length of the second array

            0.writeTo(buffer, offset = 50) // id of the first value
            11.writeTo(buffer, offset = 54) // number of the first value
            4.writeTo(buffer, offset = 58) // length of the data in the first value
            '1'.code.toShort().writeTo(buffer, offset = 60)
            '2'.code.toShort().writeTo(buffer, offset = 62)
            '3'.code.toShort().writeTo(buffer, offset = 64)
            '4'.code.toShort().writeTo(buffer, offset = 66)

            0.writeTo(buffer, offset = 68) // id of the second value
            12.writeTo(buffer, offset = 72) // number of the first value
            4.toShort().writeTo(buffer, offset = 76) // length of the data in the first value
            '1'.code.toShort().writeTo(buffer, offset = 78)
            '2'.code.toShort().writeTo(buffer, offset = 80)
            '3'.code.toShort().writeTo(buffer, offset = 82)
            '4'.code.toShort().writeTo(buffer, offset = 84)
        }

        val inputStream = ByteArrayInputStream(inputArray)
        val reader = PrimitiveValueReader(inputStream, bufferSize = 128)

        val decoder = BinarySerializationDecoder<BinarySerializationTestUtils.Keys>()

        val expectedArray1 = arrayOf(
            BinarySerializationTestUtils.Object1(id = 0, data = "1234"),
            BinarySerializationTestUtils.Object1(id = 1, data = "123"),
        )

        val expectedArray2 = arrayOf(
            BinarySerializationTestUtils.Object2(id = 0, number = 11, data = "1234"),
            BinarySerializationTestUtils.Object2(id = 0, number = 12, data = "1234"),
        )

        val objectData = decoder.decode(reader, BinarySerializationTestUtils.staticInfo)
        val actualArray1 = objectData.get { section1 } as Array<BinarySerializationTestUtils.Object1>
        val actualArray2 = objectData.get { section2 } as Array<BinarySerializationTestUtils.Object2>

        assertContentEquals(expectedArray1, actualArray1)
        assertContentEquals(expectedArray2, actualArray2)
    }
}