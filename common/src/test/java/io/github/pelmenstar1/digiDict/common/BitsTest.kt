package io.github.pelmenstar1.digiDict.common

import org.junit.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class BitsTest {
    @Test
    fun nBitsSetTest() {
        fun testCase(n: Int, expected: Int) {
            val actual = nBitsSet(n)
            assertEquals(expected, actual)
        }

        testCase(n = 1, expected = 0b1)
        testCase(n = 5, expected = 0b1_1111)
        testCase(n = 32, expected = -1)
        testCase(n = 8, expected = 0b1111_1111)
    }

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
    fun iterateSetBitsRawTest() {
        fun testCase(value: Long, expectedBitsSet: Array<Int>) {
            val list = ArrayList<Int>()

            value.iterateSetBitsRaw {
                list.add(it)
            }

            assertContentEquals(expectedBitsSet, list.toTypedArray())
        }

        testCase(
            0b00000000_00000000_00000100_00000000_00000000_00000000_10001001,
            expectedBitsSet = arrayOf(63, 60, 56, 29)
        )

        testCase(
            0b00000000_00000000_00000100_00000000_00000000_00000000_11111111,
            expectedBitsSet = arrayOf(63, 62, 61, 60, 59, 58, 57, 56, 29)
        )

        testCase(
            0b11111111_00000000_00000100_00000000_00000000_00000000_10001001,
            expectedBitsSet = arrayOf(63, 60, 56, 29, 15, 14, 13, 12, 11, 10, 9, 8)
        )

        testCase(
            0b10110111_00010100_00100100_00100000_10000000_01010011_00001001,
            expectedBitsSet = arrayOf(63, 60, 55, 54, 51, 49, 40, 34, 29, 26, 21, 19, 15, 14, 13, 11, 10, 8)
        )
    }

    @Test
    fun iterateSetBitsTest() {
        fun testCase(values: LongArray, expectedBitsSet: Array<Int>) {
            val list = ArrayList<Int>()

            values.iterateSetBits {
                list.add(it)
            }

            assertContentEquals(expectedBitsSet, list.toTypedArray())
        }

        testCase(
            longArrayOf(0b00000000_00000000_00000000_00000100_00000000_00000000_00000000_10001001),
            expectedBitsSet = arrayOf(0, 3, 7, 34)
        )

        testCase(
            longArrayOf(0b00000000_00000000_00000000_00000100_00000000_00000000_00000000_11111111),
            expectedBitsSet = arrayOf(0, 1, 2, 3, 4, 5, 6, 7, 34)
        )

        testCase(
            longArrayOf(
                0b00000000_00000000_00000000_00000100_00000000_00000000_00000000_11111111,
                0b00000000_00000000_00000000_00000000_00000000_00000000_00000000_00000001
            ),
            expectedBitsSet = arrayOf(0, 1, 2, 3, 4, 5, 6, 7, 34, 64)
        )

        testCase(
            longArrayOf(
                0b10000000_00000000_00000000_00000000_00000000_00000000_00000000_00000000UL.toLong(),
                0b00000000_00000000_00000000_00000000_00000000_00000000_00000000_00000001
            ),
            expectedBitsSet = arrayOf(63, 64)
        )
    }

    @Test
    fun iterateSetBitsFromEndExceptFirst() {
        fun testCase(values: LongArray, expectedBitsSet: Array<Int>) {
            val list = ArrayList<Int>()

            values.iterateSetBitsFromEndExceptFirst {
                list.add(it)
            }

            assertContentEquals(expectedBitsSet, list.toTypedArray())
        }

        testCase(
            longArrayOf(0b00000000_00000000_00000000_00000100_00000000_00000000_00000000_10001001),
            expectedBitsSet = arrayOf(34, 7, 3)
        )

        testCase(
            longArrayOf(0b00000000_00000000_00000000_00000100_00000000_00000000_00000000_11111111),
            expectedBitsSet = arrayOf(34, 7, 6, 5, 4, 3, 2, 1)
        )

        testCase(
            longArrayOf(0b00000000_00000000_00000000_00000100_00000000_00000000_00000000_11111110),
            expectedBitsSet = arrayOf(34, 7, 6, 5, 4, 3, 2)
        )

        testCase(
            longArrayOf(
                0b00000000_00000000_00000000_00000100_00000000_00000000_00000000_11111111,
                0b00000000_00000000_00000000_00000000_00000000_00000000_00000000_00000001
            ),
            expectedBitsSet = arrayOf(64, 34, 7, 6, 5, 4, 3, 2, 1)
        )

        testCase(
            longArrayOf(
                0b10000000_00000000_00000000_00000000_00000000_00000000_00000000_00000000UL.toLong(),
                0b00000000_00000000_00000000_00000000_00000000_00000000_00000000_00000001
            ),
            expectedBitsSet = arrayOf(64)
        )
    }

    @Test
    fun firstSetBitTest() {
        fun testCase(values: LongArray, expected: Int) {
            val actual = values.firstSetBit()

            assertEquals(expected, actual)
        }

        testCase(
            longArrayOf(0b10000000),
            expected = 7
        )

        testCase(
            longArrayOf(0b00000000),
            expected = -1
        )

        testCase(
            longArrayOf(0b00000001),
            expected = 0
        )

        testCase(
            longArrayOf(0b10000000_00000000_00000000_00000000_00000000_00000000_00000000_00000000UL.toLong()),
            expected = 63
        )

        testCase(
            longArrayOf(
                0L,
                0b00000000_00000000_00000000_00000000_00000000_00000000_00000000_00000001
            ),
            expected = 64
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