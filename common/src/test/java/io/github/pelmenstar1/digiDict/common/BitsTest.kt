package io.github.pelmenstar1.digiDict.common

import org.junit.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

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
    fun setAllBitsTest() {
        fun testCase(size: Int) {
            val bitSet = FixedBitSet(size)
            bitSet.setAll()

            for (i in 0 until size) {
                assertTrue(bitSet[i])
            }
        }

        testCase(32)
        testCase(64)
        testCase(65)
        testCase(128)
        testCase(129)
        testCase(513)
    }

    @Test
    fun setAllBitsTrueTest() {
        fun testCase(size: Int) {
            val bitSet = FixedBitSet(size)
            bitSet.setAll()

            for (i in 0 until size) {
                assertTrue(bitSet[i])
            }
        }

        testCase(1)
        testCase(32)
        testCase(64)
        testCase(65)
        testCase(128)
        testCase(129)
        testCase(513)
    }

    @Test
    fun setAllBitsFalseTest() {
        fun testCase(size: Int) {
            val bitSet = FixedBitSet(size)
            for (i in 0 until size) {
                bitSet.set(i)
            }

            bitSet.setAll(false)

            for (i in 0 until size) {
                assertFalse(bitSet[i])
            }
        }

        testCase(1)
        testCase(32)
        testCase(64)
        testCase(65)
        testCase(128)
        testCase(129)
        testCase(512)
        testCase(515)
    }

}