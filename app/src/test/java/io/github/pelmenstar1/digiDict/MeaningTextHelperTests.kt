package io.github.pelmenstar1.digiDict

import io.github.pelmenstar1.digiDict.ui.MeaningTextHelper
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class MeaningTextHelperTests {
    @Test
    fun parseToFormattedTest_common() {
        fun testCase(meaning: String, expected: String) {
            val actual = MeaningTextHelper.parseToFormatted(meaning)

            assertEquals(expected, actual)
        }

        testCase(
            meaning = "C123",
            expected = "123"
        )

        testCase(
            meaning = "C1",
            expected = "1"
        )
    }

    @Test
    fun parseToFormattedTest_list() {
        fun testCase(meaning: String, expected: String) {
            val actual = MeaningTextHelper.parseToFormatted(meaning)

            assertEquals(expected, actual)
        }

        testCase(
            meaning = "L1@M",
            expected = "• M"
        )

        testCase(
            meaning = "L1@MMM",
            expected = "• MMM"
        )

        testCase(
            meaning = "L2@MMM\nN",
            expected = "• MMM\n• N"
        )

        testCase(
            meaning = "L4@AA\nB\nCCC\nD",
            expected = "• AA\n• B\n• CCC\n• D"
        )

        testCase(
            meaning = "L2@Meaning 1\nMeaning 2",
            expected = "• Meaning 1\n• Meaning 2"
        )

        testCase(
            meaning = "L2@A lot of words\nMore words",
            expected = "• A lot of words\n• More words"
        )
    }

    @Test
    fun parseToFormattedThrowsFormatException() {
        fun testCase(input: String) {
            assertFailsWith<MeaningTextHelper.FormatException> {
                MeaningTextHelper.parseToFormatted(input)
            }
        }

        testCase("")
        testCase("123")
        testCase("L123")
        testCase("L1--@")
        testCase("@")
    }
}