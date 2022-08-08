package io.github.pelmenstar1.digiDict.common

import kotlin.test.Test
import kotlin.test.assertEquals

class TextTests {
    @Test
    fun `appendPaddedTwoDigit test`() {
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
    fun `appendPaddedFourDigit test`() {
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
    fun `parsePositiveInt returns -1 when invalid input`() {
        assertEquals(-1, "-111".parsePositiveInt())
        assertEquals(-1, "aaa".parsePositiveInt())
    }

    @Test
    fun `parsePositiveInt test`() {
        assertEquals(595, "595".parsePositiveInt())
        assertEquals(0, "0".parsePositiveInt())
        assertEquals(1, "1".parsePositiveInt())
    }

    @Test
    fun `trimToString test`() {
        assertEquals("aaa", "   aaa    ".trimToString())
        assertEquals("", "".trimToString())
        assertEquals("", "    ".trimToString())
    }

    @Test
    fun `reduceWhitespaceReplacedToSpace test`() {
        fun testCase(input: String, expected: String) {
            val actual = input.reduceNonLettersOrDigitsReplacedToSpace()

            assertEquals(expected, actual)
        }

        testCase(input = " AA BB  CC:  AA BB; JJ KK K  ;  ;", expected = "AA BB CC AA BB JJ KK K")
        testCase(input = " A;  :, B,,,,,; C ... Abcd,  ff -- mm.", expected = "A B C Abcd ff mm")
        testCase(input = "ABCD", expected = "ABCD")
        testCase(input = " AA;;;;", expected = "AA")
        testCase(input = "  ;  ., ? ;", expected = "")
        testCase(input = "A", expected = "A")
        testCase(input = "Some ordinal sentence", expected = "Some ordinal sentence")
        testCase(input = ".....;.. Some ???;;..--- ordinal   ... sentence", expected = "Some ordinal sentence")
        testCase(input = "", expected = "")
        testCase(input = ";A;", expected = "A")
    }
}