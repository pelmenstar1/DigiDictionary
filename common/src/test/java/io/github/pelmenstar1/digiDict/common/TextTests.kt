package io.github.pelmenstar1.digiDict.common

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class TextTests {
    @Test
    fun appendPaddedTwoDigitTest() {
        fun testCase(number: Int, expected: String) {
            val actual = buildString {
                appendPaddedTwoDigit(number)
            }

            assertEquals(expected, actual)
        }

        testCase(1, "01")
        testCase(5, "05")
        testCase(0, "00")
        testCase(11, "11")
        testCase(99, "99")
    }

    @Test
    fun appendPaddedFourDigitTest() {
        fun testCase(number: Int, expected: String) {
            val actual = buildString {
                appendPaddedFourDigit(number)
            }

            assertEquals(expected, actual)
        }

        testCase(1, "0001")
        testCase(5, "0005")
        testCase(0, "0000")
        testCase(11, "0011")
        testCase(99, "0099")
        testCase(100, "0100")
        testCase(1000, "1000")
        testCase(9999, "9999")
    }

    @Test
    fun parsePositiveIntReturnsMinusOneWhenInvalidInputTest() {
        assertEquals(-1, "-111".parsePositiveInt())
        assertEquals(-1, "aaa".parsePositiveInt())
    }

    @Test
    fun parsePositiveIntTest() {
        assertEquals(595, "595".parsePositiveInt())
        assertEquals(0, "0".parsePositiveInt())
        assertEquals(1, "1".parsePositiveInt())
    }

    @Test
    fun trimToStringTest() {
        assertEquals("aaa", "   aaa    ".trimToString())
        assertEquals("", "".trimToString())
        assertEquals("", "    ".trimToString())
    }

    @Test
    fun createNumberRangeListThrowsWhenMinGreaterThanMaxTest() {
        assertFailsWith(IllegalArgumentException::class) {
            createNumberRangeList(3, 2)
        }

        assertFailsWith(IllegalArgumentException::class) {
            createNumberRangeList(0, -1)
        }
    }

    @Test
    fun createNumberRangeListTest() {
        fun testCase(start: Int, endInclusive: Int, step: Int, expected: List<String>) {
            val actual = createNumberRangeList(start, endInclusive, step)

            assertEquals(expected, actual)
        }

        testCase(start = 0, endInclusive = 5, step = 1, expected = listOf("0", "1", "2", "3", "4", "5"))
        testCase(start = 5, endInclusive = 20, step = 5, expected = listOf("5", "10", "15", "20"))
        testCase(start = 3, endInclusive = 13, step = 3, expected = listOf("3", "6", "9", "12"))
    }

    @Test
    fun toStringOrEmptyTest() {
        assertEquals("", (null as Any?).toStringOrEmpty())
        assertEquals("1", 1.toStringOrEmpty())
    }

    @Test
    fun equalsByCharTest() {
        fun testCase(firstString: String?, secondString: String, expected: Boolean) {
            val actual = firstString.equalsByChar(secondString)

            assertEquals(expected, actual)
        }

        testCase("123", "123", true)
        testCase("", "123", false)
        testCase("", "", true)
        testCase(null, "123", false)
        testCase("1", "1", true)
        testCase("123", "124", false)
        testCase("123", "321", false)
        testCase("1", "123", false)
    }
}