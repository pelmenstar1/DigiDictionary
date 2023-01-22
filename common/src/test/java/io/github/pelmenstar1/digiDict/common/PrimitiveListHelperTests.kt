package io.github.pelmenstar1.digiDict.common

import org.junit.Test
import kotlin.test.assertContentEquals

class PrimitiveListHelperTests {
    private inline fun addTestHelper(
        input: LongArray,
        size: Int,
        valueToAdd: Long,
        expected: LongArray,
        addMethod: (LongArray, Int, Long) -> LongArray
    ) {
        val newArray = addMethod(input, size, valueToAdd)
        val actual = newArray.copyOf(size + 1)

        assertContentEquals(expected, actual)
    }

    @Test
    fun addLastTest() {
        fun testCase(input: LongArray, size: Int, valueToAdd: Long, expected: LongArray) {
            addTestHelper(input, size, valueToAdd, expected, PrimitiveListHelper::addLast)
        }

        testCase(input = longArrayOf(1, 2), size = 2, valueToAdd = 3, expected = longArrayOf(1, 2, 3))
        testCase(input = LongArray(0), size = 0, valueToAdd = 1, expected = longArrayOf(1))
        testCase(input = longArrayOf(1, 2, 0), size = 2, valueToAdd = 3, expected = longArrayOf(1, 2, 3))
    }

    @Test
    fun addFirstTest() {
        fun testCase(input: LongArray, size: Int, valueToAdd: Long, expected: LongArray) {
            addTestHelper(input, size, valueToAdd, expected, PrimitiveListHelper::addFirst)
        }

        testCase(input = longArrayOf(1, 2), size = 2, valueToAdd = 3, expected = longArrayOf(3, 1, 2))
        testCase(input = LongArray(0), size = 0, valueToAdd = 1, expected = longArrayOf(1))
        testCase(input = longArrayOf(1, 2, 0), size = 2, valueToAdd = 3, expected = longArrayOf(3, 1, 2))
    }
}