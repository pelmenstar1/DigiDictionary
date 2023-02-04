package io.github.pelmenstar1.digiDict.common

import org.junit.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertFailsWith

class CollectionsTests {
    @Test
    fun withAddedElementTest() {
        fun testCase(input: Array<String>, elementToAdd: String, expectedResult: Array<String>) {
            val actualResult = input.withAddedElement(elementToAdd)

            assertContentEquals(expectedResult, actualResult)
        }

        testCase(input = emptyArray(), elementToAdd = "123", expectedResult = arrayOf("123"))
        testCase(input = arrayOf("1"), elementToAdd = "2", expectedResult = arrayOf("1", "2"))
    }

    @Test
    fun withRemovedElementAtTest() {
        fun testCase(input: Array<String>, index: Int, expectedResult: Array<String>) {
            val actualResult = input.withRemovedElementAt(index)

            assertContentEquals(expectedResult, actualResult)
        }

        testCase(input = arrayOf("1"), index = 0, expectedResult = emptyArray())
        testCase(input = arrayOf("1", "2"), index = 0, expectedResult = arrayOf("2"))
        testCase(input = arrayOf("1", "2"), index = 1, expectedResult = arrayOf("1"))
        testCase(input = arrayOf("1", "2", "3"), index = 2, expectedResult = arrayOf("1", "2"))
        testCase(input = arrayOf("1", "2", "3", "4"), index = 1, expectedResult = arrayOf("1", "3", "4"))
    }

    @Test
    fun mapOffsetThrowsWhenOffsetIsNegativeTest() {
        assertFailsWith(IllegalArgumentException::class) {
            emptyArray<String>().mapOffset(-1) { it }
        }
    }

    @Test
    fun mapOffsetThrowsWhenOffsetIsGreaterThanSizeTest() {
        fun testCase(initial: Array<String>, offset: Int) {
            assertFailsWith(IllegalArgumentException::class) {
                initial.mapOffset(offset) { it }
            }
        }

        testCase(emptyArray(), 1)
        testCase(arrayOf("1", "2", "3"), 5)
    }

    @Test
    fun mapOffsetTest() {
        fun testCase(initial: Array<String>, offset: Int, expected: Array<String>) {
            val actual = initial.mapOffset(offset) { it + '0' }.toTypedArray()

            assertContentEquals(expected, actual)
        }

        testCase(initial = arrayOf("u", "a", "b", "c"), offset = 1, expected = arrayOf("a0", "b0", "c0"))
        testCase(initial = arrayOf("a", "b"), offset = 0, expected = arrayOf("a0", "b0"))
        testCase(initial = arrayOf("a"), offset = 0, expected = arrayOf("a0"))
        testCase(initial = arrayOf("a", "b", "c"), offset = 2, expected = arrayOf("c0"))
        testCase(initial = arrayOf("a", "b", "c"), offset = 3, expected = arrayOf())
    }
}