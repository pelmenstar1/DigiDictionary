package io.github.pelmenstar1.digiDict.common

import org.junit.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFails

class IntListTests {
    @Test
    fun addRepeatTest() {
        fun testCase(elements: IntArray, elementToRepeat: Int, count: Int, expectedElements: IntArray) {
            val list = IntList()
            elements.forEach { list.add(it) }

            list.addRepeat(elementToRepeat, count)
            val actualElements = list.toArray()

            assertContentEquals(expectedElements, actualElements)
        }

        testCase(
            elements = intArrayOf(),
            elementToRepeat = 1,
            count = 5,
            expectedElements = intArrayOf(1, 1, 1, 1, 1)
        )

        testCase(
            elements = intArrayOf(),
            elementToRepeat = 2,
            count = 1,
            expectedElements = intArrayOf(2)
        )

        testCase(
            elements = intArrayOf(1, 2, 3),
            elementToRepeat = 4,
            count = 3,
            expectedElements = intArrayOf(1, 2, 3, 4, 4, 4)
        )

        testCase(
            elements = intArrayOf(1, 2, 3),
            elementToRepeat = 4,
            count = 1,
            expectedElements = intArrayOf(1, 2, 3, 4)
        )

        testCase(
            elements = intArrayOf(1, 2, 3),
            elementToRepeat = 4,
            count = 0,
            expectedElements = intArrayOf(1, 2, 3)
        )
    }

    @Test
    fun addRepeatOnBufferResizeTest() {
        fun testCase(elements: IntArray, elementToRepeat: Int, count: Int, expectedElements: IntArray) {
            val list = IntList()
            list.elements = elements
            list.size = elements.size

            list.addRepeat(elementToRepeat, count)

            val actualElements = list.toArray()
            assertContentEquals(expectedElements, actualElements)
        }

        testCase(
            elements = intArrayOf(1),
            elementToRepeat = 2,
            count = 3,
            expectedElements = intArrayOf(1, 2, 2, 2)
        )

        testCase(
            elements = intArrayOf(1, 2, 3, 4, 5),
            elementToRepeat = 10,
            count = 5,
            expectedElements = intArrayOf(1, 2, 3, 4, 5, 10, 10, 10, 10, 10)
        )
    }

    @Test
    fun addRepeatThrowsOnNegativeCount() {
        assertFails {
            IntList().addRepeat(element = 0, count = -1)
        }
    }

    @Test
    fun addRangeTest() {
        fun testCase(
            elements: IntArray,
            listSize: Int,
            elementsToAdd: IntArray,
            expectedElements: IntArray
        ) {
            val list = IntList()
            list.elements = elements
            list.size = listSize

            list.addRange(elementsToAdd)

            val actualElements = list.toArray()
            assertContentEquals(expectedElements, actualElements)
        }

        testCase(
            elements = intArrayOf(),
            listSize = 0,
            elementsToAdd = intArrayOf(1, 2, 3),
            expectedElements = intArrayOf(1, 2, 3)
        )

        testCase(
            elements = intArrayOf(),
            listSize = 0,
            elementsToAdd = intArrayOf(1),
            expectedElements = intArrayOf(1)
        )

        testCase(
            elements = intArrayOf(),
            listSize = 0,
            elementsToAdd = intArrayOf(),
            expectedElements = intArrayOf()
        )

        testCase(
            elements = intArrayOf(1, 2, 0, 0),
            listSize = 2,
            elementsToAdd = intArrayOf(3, 4),
            expectedElements = intArrayOf(1, 2, 3, 4)
        )

        testCase(
            elements = intArrayOf(1, 2, 0),
            listSize = 2,
            elementsToAdd = intArrayOf(3),
            expectedElements = intArrayOf(1, 2, 3)
        )

        testCase(
            elements = intArrayOf(1, 2, 3),
            listSize = 3,
            elementsToAdd = intArrayOf(4, 5),
            expectedElements = intArrayOf(1, 2, 3, 4, 5)
        )

        testCase(
            elements = intArrayOf(1, 2, 3),
            listSize = 3,
            elementsToAdd = intArrayOf(4),
            expectedElements = intArrayOf(1, 2, 3, 4)
        )

        testCase(
            elements = intArrayOf(1, 2, 0),
            listSize = 2,
            elementsToAdd = intArrayOf(4, 5),
            expectedElements = intArrayOf(1, 2, 4, 5)
        )
    }

    @Test
    fun clearTest() {
        val list = IntList()
        list.add(0)
        list.add(1)
        list.add(2)

        assertEquals(3, list.size)
        list.clear()

        assertEquals(0, list.size)
    }

    @Test
    fun toArrayTests() {
        fun testCase(elements: IntArray, size: Int, expectedResult: IntArray) {
            val list = IntList()
            list.elements = elements
            list.size = size

            val actualResult = list.toArray()
            assertContentEquals(expectedResult, actualResult)
        }

        testCase(
            elements = intArrayOf(),
            size = 0,
            expectedResult = intArrayOf()
        )

        testCase(
            elements = intArrayOf(1, 2, 3, 4),
            size = 0,
            expectedResult = intArrayOf()
        )

        testCase(
            elements = intArrayOf(1, 2, 3, 4),
            size = 3,
            expectedResult = intArrayOf(1, 2, 3)
        )

        testCase(
            elements = intArrayOf(1, 2, 3, 4),
            size = 4,
            expectedResult = intArrayOf(1, 2, 3, 4)
        )
    }
}