package io.github.pelmenstar1.digiDict

import io.github.pelmenstar1.digiDict.utils.FixedBitSet
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class FixedBitSetTests {
    @Test
    fun getSetTest() {
        fun testCase(size: Int, setBits: IntArray) {
            val bitSet = FixedBitSet(size)

            setBits.forEach { index ->
                bitSet.set(index)

                assertTrue(bitSet[index])
            }

            // Bits that were not set should be unset.
            for (i in 0 until size) {
                if (setBits.indexOf(i) < 0) {
                    assertFalse(bitSet[i])
                }
            }
        }

        testCase(size = 4, setBits = intArrayOf(0, 3))
        testCase(size = 1, setBits = intArrayOf(0))
        testCase(size = 1, setBits = intArrayOf())
        testCase(size = 5, setBits = intArrayOf(0, 1, 2, 3, 4))
        testCase(size = 33, setBits = intArrayOf(32, 0, 31, 1))
    }

    @Test
    fun getSetConditionalTest() {
        fun testCase(size: Int, bitStates: Array<Pair<Int, Boolean>>) {
            val bitSet = FixedBitSet(size)

            bitStates.forEach { (index, isSet) ->
                bitSet[index] = isSet

                assertEquals(isSet, bitSet[index])
            }
        }

        testCase(size = 4, arrayOf(0 to false, 1 to true, 2 to true, 3 to false))
        testCase(size = 1, arrayOf(0 to false))
        testCase(size = 1, arrayOf(0 to true))
        testCase(size = 33, arrayOf(32 to true, 0 to false, 27 to true, 16 to false, 12 to true))
    }

    @Test
    fun setConditionalFalseTest() {
        fun testCase(size: Int, index: Int) {
            val bitSet = FixedBitSet(size)

            bitSet[index] = true
            assertTrue(bitSet[index])

            bitSet[index] = false
            assertFalse(bitSet[index])
        }

        testCase(size = 1, index = 0)
        testCase(size = 4, index = 3)
        testCase(size = 65, index = 64)
        testCase(size = 32, index = 31)
        testCase(size = 16, index = 8)
    }

    @Test
    fun isAllBitsSetTest() {
        fun testCase(size: Int, setBits: IntArray, expectedValue: Boolean) {
            val bitSet = FixedBitSet(size)

            setBits.forEach { bitSet.set(it) }

            assertEquals(expectedValue, bitSet.isAllBitsSet())
        }

        testCase(size = 1, setBits = intArrayOf(0), expectedValue = true)
        testCase(size = 0, setBits = intArrayOf(), expectedValue = true)
        testCase(size = 1, setBits = intArrayOf(), expectedValue = false)
        testCase(size = 4, setBits = intArrayOf(0, 1, 2, 3), expectedValue = true)
        testCase(size = 32, setBits = (0..31).toList().toIntArray(), expectedValue = true)
        testCase(size = 35, setBits = (0..34).toList().toIntArray(), expectedValue = true)
        testCase(size = 34, setBits = (0..32).toList().toIntArray(), expectedValue = false)
    }
}