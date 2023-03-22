package io.github.pelmenstar1.digiDict

import io.github.pelmenstar1.digiDict.search.RecordSearchProperty
import io.github.pelmenstar1.digiDict.search.RecordSearchPropertySet
import org.junit.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFails

class RecordSearchPropertySetTests {
    @Test
    fun constructorWithArrayTest() {
        fun testCase(elements: Array<RecordSearchProperty>, expectedBits: Int) {
            val actualBits = RecordSearchPropertySet(elements).ordinalBits

            assertEquals(expectedBits, actualBits)
        }

        testCase(emptyArray(), expectedBits = 0)
        testCase(arrayOf(RecordSearchProperty.EXPRESSION), expectedBits = 0x1)
        testCase(arrayOf(RecordSearchProperty.EXPRESSION, RecordSearchProperty.EXPRESSION), expectedBits = 0x1)
        testCase(arrayOf(RecordSearchProperty.MEANING), expectedBits = 0x2)
        testCase(arrayOf(RecordSearchProperty.MEANING, RecordSearchProperty.MEANING), expectedBits = 0x2)
        testCase(arrayOf(RecordSearchProperty.EXPRESSION, RecordSearchProperty.MEANING), expectedBits = 0x3)
    }

    @Test
    fun isEmptyTest() {
        fun testCase(elements: Array<RecordSearchProperty>, expected: Boolean) {
            val actual = RecordSearchPropertySet(elements).isEmpty()

            assertEquals(expected, actual)
        }

        testCase(emptyArray(), expected = true)
        testCase(arrayOf(RecordSearchProperty.EXPRESSION), expected = false)
        testCase(arrayOf(RecordSearchProperty.MEANING), expected = false)
        testCase(arrayOf(RecordSearchProperty.EXPRESSION, RecordSearchProperty.MEANING), expected = false)
    }

    @Test
    fun sizeTest() {
        fun testCase(elements: Array<RecordSearchProperty>, expected: Int) {
            val actual = RecordSearchPropertySet(elements).size

            assertEquals(expected, actual)
        }

        testCase(emptyArray(), expected = 0)
        testCase(arrayOf(RecordSearchProperty.EXPRESSION), expected = 1)
        testCase(arrayOf(RecordSearchProperty.MEANING), expected = 1)
        testCase(arrayOf(RecordSearchProperty.EXPRESSION, RecordSearchProperty.MEANING), expected = 2)
    }

    @Test
    fun iteratorTest() {
        fun testCase(elements: Array<RecordSearchProperty>) {
            val set = RecordSearchPropertySet(elements)

            // toTypedArray() will use iterator() to extract values
            val actualArray = set.toTypedArray()

            assertContentEquals(elements, actualArray)
        }

        testCase(emptyArray())
        testCase(arrayOf())
        testCase(arrayOf(RecordSearchProperty.EXPRESSION))
        testCase(arrayOf(RecordSearchProperty.MEANING))
        testCase(arrayOf(RecordSearchProperty.EXPRESSION, RecordSearchProperty.MEANING))
    }

    @Test
    fun toOrdinalArrayTests() {
        fun testCase(elements: Array<RecordSearchProperty>, ordinals: IntArray) {
            val actualOrdinals = RecordSearchPropertySet(elements).toOrdinalArray()

            assertContentEquals(ordinals, actualOrdinals)
        }

        testCase(emptyArray(), intArrayOf())
        testCase(arrayOf(RecordSearchProperty.EXPRESSION), intArrayOf(0))
        testCase(arrayOf(RecordSearchProperty.MEANING), intArrayOf(1))
        testCase(arrayOf(RecordSearchProperty.EXPRESSION, RecordSearchProperty.MEANING), intArrayOf(0, 1))
    }

    @Test
    fun toStringTest() {
        fun testCase(elements: Array<RecordSearchProperty>, expected: String) {
            val actual = RecordSearchPropertySet(elements).toString()

            assertEquals(expected, actual)
        }

        testCase(emptyArray(), expected = "RecordSearchPropertySet[]")
        testCase(arrayOf(RecordSearchProperty.EXPRESSION), expected = "RecordSearchPropertySet[EXPRESSION]")
        testCase(arrayOf(RecordSearchProperty.MEANING), expected = "RecordSearchPropertySet[MEANING]")
        testCase(
            arrayOf(RecordSearchProperty.EXPRESSION, RecordSearchProperty.MEANING),
            expected = "RecordSearchPropertySet[EXPRESSION, MEANING]"
        )
    }

    @Test
    fun iteratorThrowsIfNoElementsLeft() {
        fun testCase(elements: Array<RecordSearchProperty>, actions: Iterator<RecordSearchProperty>.() -> Unit) {
            val iterator = RecordSearchPropertySet(elements).iterator()

            assertFails {
                iterator.actions()
            }
        }

        testCase(emptyArray()) {
            next()
        }

        testCase(arrayOf(RecordSearchProperty.EXPRESSION)) {
            next()
            next()
        }

        testCase(arrayOf(RecordSearchProperty.EXPRESSION, RecordSearchProperty.MEANING)) {
            next()
            next()
            next()
        }
    }

}