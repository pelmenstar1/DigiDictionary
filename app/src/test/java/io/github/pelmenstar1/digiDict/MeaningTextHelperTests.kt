package io.github.pelmenstar1.digiDict

import io.github.pelmenstar1.digiDict.ui.MeaningTextHelper
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFails

class MeaningTextHelperTests {
    private fun formatTestHelper(meaning: String, expected: String) {
        val actual = MeaningTextHelper.format(meaning)

        assertEquals(expected, actual)
    }

    @Test
    fun formatCommonTest() {
        formatTestHelper(meaning = "C123", expected = "123")
        formatTestHelper(meaning = "C1", expected = "1")
    }

    @Test
    fun formatListTest() {
        formatTestHelper(meaning = "L1@M", expected = "• M")
        formatTestHelper(meaning = "L1@MMM", expected = "• MMM")
        formatTestHelper(meaning = "L2@MMM\nN", expected = "• MMM\n• N")
        formatTestHelper(meaning = "L4@AA\nB\nCCC\nD", expected = "• AA\n• B\n• CCC\n• D")
        formatTestHelper(meaning = "L2@Meaning 1\nMeaning 2", expected = "• Meaning 1\n• Meaning 2")
        formatTestHelper(meaning = "L2@A lot of words\nMore words", expected = "• A lot of words\n• More words")
    }

    @Test
    fun formatThrowsExceptionTest() {
        fun testCase(input: String) {
            assertFails { MeaningTextHelper.format(input) }
        }

        testCase("")
        testCase("123")
        testCase("L123")
        testCase("L1--@")
        testCase("@")
    }
}