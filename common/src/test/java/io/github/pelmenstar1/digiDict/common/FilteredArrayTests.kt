package io.github.pelmenstar1.digiDict.common

import io.github.pelmenstar1.digiDict.commonTestUtils.FilteredArrayTestUtils
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals

class FilteredArrayTests {
    @Test
    fun sizeTest() {
        fun testCase(originSize: Int, passedIndices: IntArray) {
            val origin = arrayOfNulls<Any>(originSize)

            val bitSet = FilteredArrayTestUtils.createBitSet(originSize, passedIndices)
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

            val bitSet = FilteredArrayTestUtils.createBitSet(originSize, passedIndices)
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

    @Test
    fun sortTest() {
        fun testCase(input: Array<Int>, passedIndices: IntArray, output: Array<Int>) {
            val bitSet = FilteredArrayTestUtils.createBitSet(input.size, passedIndices)
            val filteredInput = FilteredArray(input, bitSet)
            val filteredOutput = filteredInput.sorted { a, b -> a.compareTo(b) }
            val actualOutput = filteredOutput.toArray()

            assertContentEquals(output, actualOutput)
        }


        testCase(
            input = arrayOf(5, 4, 3, 2, 1),
            passedIndices = intArrayOf(0, 1, 2, 3, 4),
            output = arrayOf(1, 2, 3, 4, 5)
        )

        testCase(
            input = arrayOf(1),
            passedIndices = intArrayOf(0),
            output = arrayOf(1)
        )

        testCase(
            input = arrayOf(9, 3, 6, 5, 0),
            passedIndices = intArrayOf(0, 1, 2, 3, 4),
            output = arrayOf(0, 3, 5, 6, 9)
        )

        testCase(
            input = arrayOf(3, 2, 5, 9, 0, 100, 6, 7, 9),
            passedIndices = intArrayOf(0, 2, 4, 6, 8),
            output = arrayOf(0, 3, 5, 6, 9)
        )

        testCase(
            input = arrayOf(1, 2, 4, 3),
            passedIndices = intArrayOf(2, 3),
            output = arrayOf(3, 4)
        )

        testCase(
            input = arrayOf(1, 2, 3, 4),
            passedIndices = intArrayOf(0, 1, 2, 3),
            output = arrayOf(1, 2, 3, 4)
        )

        testCase(
            input = arrayOf(1, 2, 3, 4),
            passedIndices = intArrayOf(2, 3),
            output = arrayOf(3, 4)
        )

        testCase(
            input = arrayOf(100, 50, 70, 40, 80, 90, 110, 0),
            passedIndices = intArrayOf(0, 1, 2, 3, 4, 5, 6, 7),
            output = arrayOf(0, 40, 50, 70, 80, 90, 100, 110)
        )

        testCase(
            input = arrayOf(100, 50, 70, 40, 80, 90, 110, 0),
            passedIndices = intArrayOf(1, 2, 3, 4, 5, 6),
            output = arrayOf(40, 50, 70, 80, 90, 110)
        )
    }

    private inline fun <reified T> FilteredArray<T>.toArray(): Array<T> {
        val array = unsafeNewArray<T>(size)

        for (i in 0 until size) {
            array[i] = this[i]
        }

        return array
    }
}