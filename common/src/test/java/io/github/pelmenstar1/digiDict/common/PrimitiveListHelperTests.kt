package io.github.pelmenstar1.digiDict.common

import org.junit.Test
import kotlin.test.assertContentEquals

class PrimitiveListHelperTests {
    private inline fun <TValue, TArray> addTestHelper(
        input: TArray,
        size: Int,
        valueToAdd: TValue,
        expected: TArray,
        addMethod: (TArray, Int, TValue) -> TArray,
        copyOf: TArray.(newSize: Int) -> TArray,
        assertArrContentEquals: (TArray, TArray) -> Unit
    ) {
        val newArray = addMethod(input, size, valueToAdd)
        val actual = newArray.copyOf(size + 1)

        assertArrContentEquals(expected, actual)
    }

    @Test
    fun longArray_addLastTest() {
        fun testCase(input: LongArray, size: Int, valueToAdd: Long, expected: LongArray) {
            addTestHelper(
                input, size, valueToAdd, expected,
                PrimitiveListHelper::addLast, LongArray::copyOf, ::assertContentEquals
            )
        }

        testCase(input = longArrayOf(1, 2), size = 2, valueToAdd = 3, expected = longArrayOf(1, 2, 3))
        testCase(input = longArrayOf(), size = 0, valueToAdd = 1, expected = longArrayOf(1))
        testCase(input = longArrayOf(1, 2, 0), size = 2, valueToAdd = 3, expected = longArrayOf(1, 2, 3))
    }

    @Test
    fun intArray_addLastTest() {
        fun testCase(input: IntArray, size: Int, valueToAdd: Int, expected: IntArray) {
            addTestHelper(
                input, size, valueToAdd, expected,
                PrimitiveListHelper::addLast, IntArray::copyOf, ::assertContentEquals
            )
        }

        testCase(input = intArrayOf(1, 2), size = 2, valueToAdd = 3, expected = intArrayOf(1, 2, 3))
        testCase(input = intArrayOf(), size = 0, valueToAdd = 1, expected = intArrayOf(1))
        testCase(input = intArrayOf(1, 2, 0), size = 2, valueToAdd = 3, expected = intArrayOf(1, 2, 3))
    }

    @Test
    fun longArray_addFirstTest() {
        fun testCase(input: LongArray, size: Int, valueToAdd: Long, expected: LongArray) {
            addTestHelper(
                input, size, valueToAdd, expected,
                PrimitiveListHelper::addFirst, LongArray::copyOf, ::assertContentEquals
            )
        }

        testCase(input = longArrayOf(1, 2), size = 2, valueToAdd = 3, expected = longArrayOf(3, 1, 2))
        testCase(input = longArrayOf(), size = 0, valueToAdd = 1, expected = longArrayOf(1))
        testCase(input = longArrayOf(1, 2, 0), size = 2, valueToAdd = 3, expected = longArrayOf(3, 1, 2))
    }

    @Test
    fun intArray_addFirstTest() {
        fun testCase(input: IntArray, size: Int, valueToAdd: Int, expected: IntArray) {
            addTestHelper(
                input, size, valueToAdd, expected,
                PrimitiveListHelper::addFirst, IntArray::copyOf, ::assertContentEquals
            )
        }

        testCase(input = intArrayOf(1, 2), size = 2, valueToAdd = 3, expected = intArrayOf(3, 1, 2))
        testCase(input = intArrayOf(), size = 0, valueToAdd = 1, expected = intArrayOf(1))
        testCase(input = intArrayOf(1, 2, 0), size = 2, valueToAdd = 3, expected = intArrayOf(3, 1, 2))
    }
}