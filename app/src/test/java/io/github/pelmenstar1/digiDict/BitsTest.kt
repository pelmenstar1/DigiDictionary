package io.github.pelmenstar1.digiDict

import io.github.pelmenstar1.digiDict.utils.findPositionOfNthSetBit
import io.github.pelmenstar1.digiDict.utils.iterateSetBits
import io.github.pelmenstar1.digiDict.utils.lowestNBitsSetInt
import org.junit.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals

class BitsTest {
    @Test
    fun findPositionOfNthBitInLongTest() {
        fun testCase(value: Long, n: Int, expectedPosition: Int) {
            val actualPosition = value.findPositionOfNthSetBit(n)

            assertEquals(expectedPosition, actualPosition)
        }

        testCase(0b00000000_00000000_00000100_00000000_00000000_00000000_10001001, 2, 7)
        testCase(0b10010000_00000001_00000000_00000000_00000010_00100000_10001000, 3, 17)
        testCase(0b00000000_00000000_00000000_00000000_00000000_00000000_00001000, 2, -1)
    }

    @Test
    fun findPositionOfNthSetBitInLongArrayTest() {
        fun testCase(value: LongArray, n: Int, expectedPosition: Int) {
            val actualPosition = value.findPositionOfNthSetBit(n)

            assertEquals(expectedPosition, actualPosition)
        }

        testCase(
            longArrayOf(
                0b00000000_00000000_00000000_00000000_00000000_00000000_10001001,
                0b00000000_00000000_00000000_00000000_00000000_00000000_00000001
            ), 3, 64
        )
    }


    @Test
    fun iterateSetBitsTest() {
        fun testCase(value: Long, expectedBitsSet: Array<Int>) {
            val list = ArrayList<Int>()

            value.iterateSetBits {
                list.add(it)
            }

            assertContentEquals(expectedBitsSet, list.toTypedArray())
        }

        testCase(
            0b00000000_00000000_00000100_00000000_00000000_00000000_10001001,
            expectedBitsSet = arrayOf(0, 3, 7, 34)
        )

        testCase(
            0b00000000_00000000_00000100_00000000_00000000_00000000_11111111,
            expectedBitsSet = arrayOf(0, 1, 2, 3, 4, 5, 6, 7, 34)
        )

        testCase(
            0b11111111_00000000_00000100_00000000_00000000_00000000_10001001,
            expectedBitsSet = arrayOf(0, 3, 7, 34, 48, 49, 50, 51, 52, 53, 54, 55)
        )

        testCase(
            0b10110111_00010100_00100100_00100000_10000000_01010011_00001001,
            expectedBitsSet = arrayOf(0, 3, 8, 9, 12, 14, 23, 29, 34, 37, 42, 44, 48, 49, 50, 52, 53, 55)
        )
    }

    @Test
    fun lowestNBitsSetTest() {
        assertEquals(0, lowestNBitsSetInt(0))
        assertEquals(1, lowestNBitsSetInt(1))
        assertEquals(0b11, lowestNBitsSetInt(2))
        assertEquals(0b111, lowestNBitsSetInt(3))
        assertEquals(0b1111, lowestNBitsSetInt(4))
        assertEquals(0b11111, lowestNBitsSetInt(5))
        assertEquals(0xFFFF, lowestNBitsSetInt(16))
        assertEquals((-1), lowestNBitsSetInt(32))
        assertEquals((-1), lowestNBitsSetInt(33))
    }
}