package io.github.pelmenstar1.digiDict.common

import kotlin.test.*

class TextTests {
    private class CharSequenceImpl(val data: String) : CharSequence {
        override val length: Int
            get() = data.length

        override fun get(index: Int) = data[index]

        override fun subSequence(startIndex: Int, endIndex: Int): CharSequence {
            return CharSequenceImpl(data.substring(startIndex, endIndex))
        }

        override fun toString() = data
    }

    private fun paddedTwoDigitTestCases(testCase: (number: Int, expected: String) -> Unit) {
        testCase(1, "01")
        testCase(5, "05")
        testCase(0, "00")
        testCase(11, "11")
        testCase(99, "99")
    }

    @Test
    fun appendPaddedTwoDigitTest() {
        paddedTwoDigitTestCases { number, expected ->
            val actual = buildString {
                appendPaddedTwoDigit(number)
            }

            assertEquals(expected, actual)
        }
    }

    @Test
    fun writePaddedTwoDigitTest() {
        paddedTwoDigitTestCases { number, expected ->
            val buffer = CharArray(10) { 'a' + it }

            buffer.writePaddedTwoDigit(number, offset = 1)

            val actual = String(buffer, 1, 2)
            assertEquals(expected, actual)

            // Check whether the other buffer content remains untouched
            assertEquals('a', buffer[0], "index: 0")

            for (i in 3 until 10) {
                assertEquals('a' + i, buffer[i], "index: $i")
            }
        }
    }

    private fun paddedFourDigitTestCases(testCase: (number: Int, expected: String) -> Unit) {
        testCase(1, "0001")
        testCase(5, "0005")
        testCase(0, "0000")
        testCase(11, "0011")
        testCase(99, "0099")
        testCase(100, "0100")
        testCase(101, "0101")
        testCase(1000, "1000")
        testCase(1234, "1234")
        testCase(9999, "9999")
    }

    @Test
    fun appendPaddedFourDigitTest() {
        paddedFourDigitTestCases { number, expected ->
            val actual = buildString {
                appendPaddedFourDigit(number)
            }

            assertEquals(expected, actual)
        }
    }

    @Test
    fun writePaddedFourDigitTest() {
        paddedFourDigitTestCases { number, expected ->
            val buffer = CharArray(10) { 'a' + it }
            buffer.writePaddedFourDigit(number, offset = 1)

            val actual = String(buffer, 1, 4)
            assertEquals(expected, actual)

            // Check whether the other buffer content remains untouched
            assertEquals('a', buffer[0])
            for (i in 5 until 10) {
                assertEquals('a' + i, buffer[i], "index: $i")
            }
        }
    }

    @Test
    fun parsePositiveIntOnInvalidInputTest() {
        assertEquals(-1, "-111".parsePositiveInt())
        assertEquals(-1, "aaa".parsePositiveInt())
        assertEquals(-1, "123".parsePositiveInt(start = 1, end = 1))
    }

    @Test
    fun parsePositiveTest() {
        assertEquals(595, "595".parsePositiveInt())
        assertEquals(0, "0".parsePositiveInt())
        assertEquals(1, "1".parsePositiveInt())
        assertEquals(123, " 123ABC".parsePositiveInt(start = 1, end = 4))
        assertEquals(Int.MAX_VALUE, Int.MAX_VALUE.toString().parsePositiveInt())
    }

    @Test
    fun trimToStringTest() {
        assertEquals("aaa", "   aaa    ".trimToString())
        assertEquals("", "".trimToString())
        assertEquals("", "    ".trimToString())
        assertEquals("", (null as CharSequence?).trimToString())
    }

    @Test
    fun createNumberRangeListThrowsWhenMinIsGreaterThanMax() {
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
    fun nextLetterOrDigitTest() {
        fun testCase(text: String, start: Int, end: Int, expectedIndex: Int) {
            val actualIndex = text.nextLetterOrDigitIndex(start, end)

            assertEquals(expectedIndex, actualIndex)
        }

        testCase(text = "  A", start = 0, end = 3, expectedIndex = 2)
        testCase(text = "A  ", start = 0, end = 3, expectedIndex = 0)
        testCase(text = "A  ", start = 0, end = 1, expectedIndex = 0)
        testCase(text = "  A", start = 0, end = 2, expectedIndex = -1)
        testCase(text = "A A", start = 1, end = 3, expectedIndex = 2)
        testCase(text = "A", start = 0, end = 1, expectedIndex = 0)
    }

    @Test
    fun nextNonLetterOrDigitTest() {
        fun testCase(text: String, start: Int, end: Int, expectedIndex: Int) {
            val actualIndex = text.nextNonLetterOrDigitIndex(start, end)

            assertEquals(expectedIndex, actualIndex)
        }

        testCase(text = "AA_", start = 0, end = 3, expectedIndex = 2)
        testCase(text = "_", start = 0, end = 1, expectedIndex = 0)
        testCase(text = "__A_", start = 1, end = 4, expectedIndex = 1)
        testCase(text = "_A_", start = 1, end = 3, expectedIndex = 2)
    }

    @Test
    fun containsLetterOrDigitTest() {
        assertTrue("123".containsLetterOrDigit())
        assertFalse(" ".containsLetterOrDigit())
        assertTrue("   ABC   ".containsLetterOrDigit())
    }

    @Test
    fun subSequenceToStringOnStringsTest() {
        assertEquals("123", "A123B".subSequenceToString(1, 4))
        assertEquals("", "123".subSequenceToString(0, 0))
        assertEquals("1", "123".subSequenceToString(0, 1))
    }

    @Test
    fun subSequenceToStringOnCharSequenceImplTest() {
        fun testCase(input: String, start: Int, end: Int, expected: String) {
            val actual = CharSequenceImpl(input).subSequenceToString(start, end)

            assertEquals(expected, actual)
        }

        testCase(input = "A123B", start = 1, end = 4, expected = "123")
        testCase(input = "123", start = 0, end = 0, expected = "")
        testCase(input = "123", start = 0, end = 1, expected = "1")
    }

    @Test
    fun decimalDigitCountTest() {
        fun testCase(number: Int, expected: Int) {
            val actual = number.decimalDigitCount()

            assertEquals(expected, actual, "number: $number")
        }

        testCase(number = 1, expected = 1)
        testCase(number = 5, expected = 1)
        testCase(number = 10, expected = 2)
        testCase(number = 16, expected = 2)
        testCase(number = 50, expected = 2)
        testCase(number = 99, expected = 2)
        testCase(number = 100, expected = 3)
        testCase(number = 101, expected = 3)
        testCase(number = 301, expected = 3)
        testCase(number = 2023, expected = 4)
        testCase(number = 1001, expected = 4)
        testCase(number = 9999, expected = 4)
        testCase(number = 12345, expected = 5)
        testCase(number = 10000, expected = 5)
        testCase(number = 99999, expected = 5)
        testCase(number = 123456, expected = 6)
        testCase(number = 100000, expected = 6)
        testCase(number = 999999, expected = 6)
        testCase(number = 1234567, expected = 7)
        testCase(number = 1000000, expected = 7)
        testCase(number = 1000012, expected = 7)
        testCase(number = 9999999, expected = 7)
        testCase(number = 10000000, expected = 8)
        testCase(number = 12345678, expected = 8)
        testCase(number = 99999999, expected = 8)
        testCase(number = 123456789, expected = 9)
        testCase(number = 999999999, expected = 9)
        testCase(number = 123456789, expected = 9)
        testCase(number = 2147483646, expected = 10)
        testCase(number = Int.MAX_VALUE, expected = 10)
    }
}