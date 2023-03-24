package io.github.pelmenstar1.digiDict.common

import org.junit.Test
import kotlin.random.Random
import kotlin.test.assertEquals
import kotlin.test.assertFails
import kotlin.test.assertTrue

class RandomTests {
    private fun isElementsUnique(elements: IntArray): Boolean {
        elements.sort()

        for (i in 1 until elements.size) {
            if (elements[i - 1] == elements[i]) {
                return false
            }
        }

        return true
    }

    @Test
    fun generateUniqueRandomNumbersShouldThrowWhenUpperBoundIsLessThanSize() {
        assertFails {
            Random.generateUniqueRandomNumbers(upperBound = 5, size = 10)
        }
    }

    @Test
    fun generateUniqueRandomNumbersShouldBeActuallyUniqueTest() {
        fun testCase(upperBound: Int, size: Int) {
            repeat(50) {
                val result = Random.generateUniqueRandomNumbers(upperBound, size)

                assertTrue(isElementsUnique(result))
            }
        }

        testCase(upperBound = 1000, size = 100)
        testCase(upperBound = 10, size = 10)
        testCase(upperBound = 1, size = 1)
    }

    @Test
    fun generateUniqueRandomNumbersShouldReturnArrayWithSizeAsExpectedTest() {
        assertEquals(5, Random.generateUniqueRandomNumbers(upperBound = 10, size = 5).size)
        assertEquals(10, Random.generateUniqueRandomNumbers(upperBound = 10, size = 10).size)
        assertEquals(1, Random.generateUniqueRandomNumbers(upperBound = 10, size = 1).size)
    }

    @Test
    fun generateUniqueRandomNumberShouldBeWithinRangeTest() {
        fun testCase(upperBound: Int, size: Int) {
            repeat(50) {
                val result = Random.generateUniqueRandomNumbers(upperBound, size)

                assertTrue(result.all { it in 0 until upperBound })
            }
        }

        testCase(upperBound = 1000, size = 100)
        testCase(upperBound = 1, size = 1)
        testCase(upperBound = 10, size = 10)
    }

    @Test
    fun generateUniqueRandomNumbersOnSizeOrUpperBoundZeroTest() {
        assertTrue(Random.generateUniqueRandomNumbers(upperBound = 0, size = 0).isEmpty())
    }
}