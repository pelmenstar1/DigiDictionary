package io.github.pelmenstar1.digiDict.common

import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.pelmenstar1.digiDict.common.binarySerialization.*
import org.junit.Test
import org.junit.runner.RunWith
import java.io.ByteArrayOutputStream
import kotlin.test.assertEquals

@RunWith(AndroidJUnit4::class)
class BinarySerializationEncoderTests {
    @Test
    fun encodeTest() {
        val outputStream = ByteArrayOutputStream()
        val writer = PrimitiveValueWriter(outputStream, bufferSize = 16)

        val array1 = arrayOf(
            BinarySerializationTestUtils.Object1(id = 0, data = "1234"),
            BinarySerializationTestUtils.Object1(id = 1, data = "123"),
        )

        val array2 = arrayOf(
            BinarySerializationTestUtils.Object2(id = 0, number = 11, data = "1234"),
            BinarySerializationTestUtils.Object2(id = 0, number = 12, data = "1234"),
        )

        val objectData = BinarySerializationObjectData(BinarySerializationTestUtils.staticInfo) {
            put(BinarySerializationTestUtils.Keys.section1, array1)
            put(BinarySerializationTestUtils.Keys.section2, array2)
        }

        val encoder = BinarySerializationEncoder<BinarySerializationTestUtils.Keys>()
        encoder.encode(objectData, writer)
        writer.flush()

        val result = outputStream.toByteArray()

        assertEquals(BinarySerializationConstants.MAGIC_WORD, result.readLong(offset = 0))

        assertEquals(1, result.readInt(offset = 8)) // latest version of serializer for Object1
        assertEquals(2, result.readInt(offset = 12)) // length of the first array

        assertEquals(0, result.readInt(offset = 16)) // id of the first value
        assertEquals(4, result.readShort(offset = 20)) // length of the data in the first value
        assertEquals('1'.code, result.readShort(offset = 22).toInt())
        assertEquals('2'.code, result.readShort(offset = 24).toInt())
        assertEquals('3'.code, result.readShort(offset = 26).toInt())
        assertEquals('4'.code, result.readShort(offset = 28).toInt())

        assertEquals(1, result.readInt(offset = 30)) // id of the second value
        assertEquals(3, result.readShort(offset = 34)) // length of the data in the second value
        assertEquals('1'.code, result.readShort(offset = 36).toInt())
        assertEquals('2'.code, result.readShort(offset = 38).toInt())
        assertEquals('3'.code, result.readShort(offset = 40).toInt())

        assertEquals(2, result.readInt(offset = 42)) // latest version of serializer for Object2
        assertEquals(2, result.readInt(offset = 46)) // length of the second array

        assertEquals(0, result.readInt(offset = 50)) // id of the first value
        assertEquals(11, result.readInt(offset = 54)) // number of the first value
        assertEquals(4, result.readShort(offset = 58)) // length of the data in the first value
        assertEquals('1'.code, result.readShort(offset = 60).toInt())
        assertEquals('2'.code, result.readShort(offset = 62).toInt())
        assertEquals('3'.code, result.readShort(offset = 64).toInt())
        assertEquals('4'.code, result.readShort(offset = 66).toInt())

        assertEquals(0, result.readInt(offset = 68)) // id of the second value
        assertEquals(12, result.readInt(offset = 72)) // number of the first value
        assertEquals(4, result.readShort(offset = 76)) // length of the data in the first value
        assertEquals('1'.code, result.readShort(offset = 78).toInt())
        assertEquals('2'.code, result.readShort(offset = 80).toInt())
        assertEquals('3'.code, result.readShort(offset = 82).toInt())
        assertEquals('4'.code, result.readShort(offset = 84).toInt())
    }
}