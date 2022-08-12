package io.github.pelmenstar1.digiDict.common

import org.junit.Test
import kotlin.test.assertEquals

class MathUtilsTests {
    @Test
    fun nextPowerOf2Test() {
        fun testCase(number: Int, expected: Int) {
            val actual = number.nextPowerOf2()

            assertEquals(expected, actual)
        }

        testCase(number = 1, expected = 1)
        testCase(number = 2, expected = 2)
        testCase(number = 3, expected = 4)
        testCase(number = 4, expected = 4)
        testCase(number = 13, expected = 16)
        testCase(number = 21, expected = 32)
        testCase(number = 33, expected = 64)
        testCase(number = 1000, expected = 1024)
        testCase(number = 1200, expected = 2048)
    }
}