package io.github.pelmenstar1.digiDict

import io.github.pelmenstar1.digiDict.utils.findPositionOfNthSetBit
import io.github.pelmenstar1.digiDict.utils.lowestNBitsSet
import org.junit.Test
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
    fun lowestNBitsSetTest() {
        assertEquals(0, lowestNBitsSet(0))
        assertEquals(1, lowestNBitsSet(1))
        assertEquals(0b11, lowestNBitsSet(2))
        assertEquals(0b111, lowestNBitsSet(3))
        assertEquals(0b1111, lowestNBitsSet(4))
        assertEquals(0b11111, lowestNBitsSet(5))
        assertEquals(0xFFFF, lowestNBitsSet(16))
        assertEquals((-1), lowestNBitsSet(32))
        assertEquals((-1), lowestNBitsSet(33))
    }
}