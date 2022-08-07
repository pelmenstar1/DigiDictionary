package io.github.pelmenstar1.digiDict

import io.github.pelmenstar1.digiDict.utils.FilteredArray
import kotlin.test.Test
import kotlin.test.assertEquals

class FilteredArrayTests {
    private fun createBitSet(size: Int, indices: IntArray): LongArray {
        val wordCount = (size + 63) / 64
        val bitSet = LongArray(wordCount)
        for (index in indices) {
            val wordIndex = index / 64
            bitSet[wordIndex] = bitSet[wordIndex] or (1L shl index)
        }

        return bitSet
    }

    @Test
    fun sizeTest() {
        fun testCase(originSize: Int, passedIndices: IntArray) {
            val origin = arrayOfNulls<Any>(originSize)

            val bitSet = createBitSet(originSize, passedIndices)
            val filteredArray = FilteredArray(origin, bitSet)

            assertEquals(passedIndices.size, filteredArray.size)
        }

        testCase(originSize = 32, passedIndices = intArrayOf(0, 16, 31))
        testCase(originSize = 64, passedIndices = intArrayOf(1, 12, 63, 5))
        testCase(originSize = 127, passedIndices = intArrayOf(0, 1, 126, 64, 63))
    }

    @Test
    fun getTest() {
        fun testCase(originSize: Int, passedIndices: IntArray) {
            passedIndices.sort()

            val passedElements = Array(passedIndices.size) { Any() }
            var pIndex = 0
            val origin = Array(originSize) { i ->
                if (passedIndices.contains(i)) {
                    passedElements[pIndex++]
                } else {
                    null
                }
            }

            val bitSet = createBitSet(originSize, passedIndices)
            val filteredArray = FilteredArray(origin, bitSet)

            pIndex = 0
            for (i in passedIndices.indices) {
                assertEquals(passedElements[pIndex++], filteredArray[i])
            }
        }

        testCase(1, passedIndices = intArrayOf(0))
        testCase(32, passedIndices = intArrayOf(0, 1, 2, 5, 6, 10, 31, 12, 17))
        testCase(64, passedIndices = intArrayOf(1, 2, 3, 4, 60, 32, 5))
        testCase(127, passedIndices = intArrayOf(0, 1, 2, 120, 4, 64, 78))
    }
}