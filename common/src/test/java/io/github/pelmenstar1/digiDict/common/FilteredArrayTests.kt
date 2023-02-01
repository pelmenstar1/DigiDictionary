package io.github.pelmenstar1.digiDict.common

import io.github.pelmenstar1.digiDict.commonTestUtils.toArray
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFails

class FilteredArrayTests {
    @Test
    fun getThrowsOutOfBounds() {
        assertFails {
            FilteredArray(arrayOf(1, 2), size = 1)[1]
        }
    }

    @Test
    fun getTest() {
        fun testCase(elements: Array<Int>, size: Int, index: Int, expectedElement: Int) {
            val array = FilteredArray(elements, size)

            val actualElement = array[index]
            assertEquals(expectedElement, actualElement)
        }

        testCase(elements = arrayOf(1), size = 1, index = 0, expectedElement = 1)
        testCase(elements = arrayOf(1, 2, 3), size = 2, index = 1, expectedElement = 2)
    }

    @Test
    fun sortTest() {
        fun testCase(input: Array<Int>, size: Int = input.size, output: Array<Int>) {
            val filteredInput = FilteredArray(input, size)
            filteredInput.sort(Int::compareTo)

            val actualOutput = filteredInput.toArray()

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

    @Test
    fun equalsTest() {
        fun testCase(
            firstData: Array<Int>, firstSize: Int,
            secondData: Array<Int>, secondSize: Int,
            expectedResult: Boolean
        ) {
            val firstArray = FilteredArray(firstData, firstSize)
            val secondArray = FilteredArray(secondData, secondSize)

            val actualResult = firstArray == secondArray
            assertEquals(expectedResult, actualResult)
        }

        testCase(
            firstData = arrayOf(1, 2, 3, 0), firstSize = 3,
            secondData = arrayOf(1, 2, 3, 4), secondSize = 3,
            expectedResult = true
        )

        testCase(
            firstData = arrayOf(1, 2, 3, 4), firstSize = 4,
            secondData = arrayOf(1, 2, 3, 5), secondSize = 4,
            expectedResult = false
        )

        testCase(
            firstData = arrayOf(1, 2), firstSize = 2,
            secondData = arrayOf(1, 2), secondSize = 1,
            expectedResult = false
        )

        testCase(
            firstData = emptyArray(), firstSize = 0,
            secondData = emptyArray(), secondSize = 0,
            expectedResult = true
        )

        testCase(
            firstData = arrayOf(1, 2), firstSize = 0,
            secondData = arrayOf(1), secondSize = 0,
            expectedResult = true
        )

        testCase(
            firstData = arrayOf(1), firstSize = 1,
            secondData = arrayOf(1), secondSize = 1,
            expectedResult = true
        )
    }

    @Test
    fun hashCodeShouldBeEqualWhenArrayEqualTest() {
        fun testCase(
            firstData: Array<Int>, firstSize: Int,
            secondData: Array<Int>, secondSize: Int,
        ) {
            val array1 = FilteredArray(firstData, firstSize)
            val array2 = FilteredArray(secondData, secondSize)

            val hash1 = array1.hashCode()
            val hash2 = array2.hashCode()

            assertEquals(hash1, hash2)
        }

        testCase(
            firstData = emptyArray(), firstSize = 0,
            secondData = emptyArray(), secondSize = 0
        )

        testCase(
            firstData = arrayOf(1, 2, 3), firstSize = 3,
            secondData = arrayOf(1, 2, 3, 4), secondSize = 3
        )

        testCase(
            firstData = arrayOf(1, 2, 3), firstSize = 3,
            secondData = arrayOf(1, 2, 3), secondSize = 3
        )
    }

    @Test
    fun toStringTest() {
        fun testCase(elements: Array<Int>, size: Int, expectedResult: String) {
            val array = FilteredArray(elements, size)

            val actualResult = array.toString()
            assertEquals(expectedResult, actualResult)
        }

        testCase(elements = arrayOf(), size = 0, expectedResult = "FilteredArray(size=0, elements=[])")
        testCase(elements = arrayOf(1, 2, 3), size = 0, expectedResult = "FilteredArray(size=0, elements=[])")
        testCase(elements = arrayOf(1, 2, 3), size = 2, expectedResult = "FilteredArray(size=2, elements=[1, 2])")
        testCase(elements = arrayOf(1, 2, 3), size = 3, expectedResult = "FilteredArray(size=3, elements=[1, 2, 3])")
    }
}