package io.github.pelmenstar1.digiDict.common

import io.github.pelmenstar1.digiDict.commonTestUtils.toArray
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertFails

class FilteredArrayTests {
    @Test
    fun getThrowsOutOfBounds() {
        assertFails {
            FilteredArray(arrayOf(1, 2), size = 1)[1]
        }
    }

    @Test
    fun sortTest() {
        fun testCase(input: Array<Int>, size: Int = input.size, output: Array<Int>) {
            val filteredInput = FilteredArray(input, size)
            val filteredOutput = filteredInput.sorted(Int::compareTo)
            val actualOutput = filteredOutput.toArray()

            assertContentEquals(output, actualOutput)
        }

        testCase(
            input = arrayOf(5, 4, 3, 2, 1),
            output = arrayOf(1, 2, 3, 4, 5)
        )

        testCase(
            input = arrayOf(1),
            output = arrayOf(1)
        )

        testCase(
            input = arrayOf(9, 3, 6, 5, 0),
            output = arrayOf(0, 3, 5, 6, 9)
        )

        testCase(
            input = arrayOf(3, 2, 5, 9, 0, 6, 7, 9, 100),
            size = 5,
            output = arrayOf(0, 2, 3, 5, 9)
        )

        testCase(
            input = arrayOf(4, 2, 1, 2),
            size = 2,
            output = arrayOf(2, 4)
        )

        testCase(
            input = arrayOf(1, 2, 3, 4),
            output = arrayOf(1, 2, 3, 4)
        )

        testCase(
            input = arrayOf(100, 50, 70, 40, 80, 90, 110, 0),
            output = arrayOf(0, 40, 50, 70, 80, 90, 100, 110)
        )

        testCase(
            input = arrayOf(50, 70, 40, 80, 90, 110, 0, 100),
            size = 6,
            output = arrayOf(40, 50, 70, 80, 90, 110)
        )
    }

    @Test
    fun toFilteredArrayTest() {
        fun testCase(elements: Array<Int>, expected: Array<Int>) {
            val filteredArray = elements.toFilteredArray { it > 10 }
            val actual = filteredArray.toArray()

            assertContentEquals(expected, actual)
        }

        testCase(elements = emptyArray(), expected = emptyArray())
        testCase(elements = arrayOf(1, 2, 3, 11), expected = arrayOf(11))
        testCase(elements = arrayOf(1, 2, 3, 12, 13, 14, 0, 5), expected = arrayOf(12, 13, 14))
        testCase(elements = arrayOf(1, 2, 3), expected = emptyArray())
        testCase(elements = arrayOf(11, 12, 13), expected = arrayOf(11, 12, 13))
        testCase(elements = arrayOf(12, 15, 0, 1, 11), expected = arrayOf(12, 15, 11))
        testCase(elements = arrayOf(11, 12, 13, 0, 1, 2), expected = arrayOf(11, 12, 13))
    }
}