package io.github.pelmenstar1.digiDict

import io.github.pelmenstar1.digiDict.data.ComplexMeaning
import org.junit.Test
import kotlin.test.assertEquals

class ComplexMeaningTests {
    @Test
    fun `create common test`() {
        assertEquals(ComplexMeaning.Common("123").rawText, "C123")
        assertEquals(ComplexMeaning.Common("ABCD").rawText, "CABCD")
        assertEquals(ComplexMeaning.Common("").rawText, "C")
    }

    @Test
    fun `create list test`() {
        @Suppress("UNCHECKED_CAST")
        fun testCase(expected: String, vararg elements: String) {
            assertEquals(expected, ComplexMeaning.List(elements as Array<String>).rawText)
        }

        testCase("L3@E_1\nE_2\nE_3", "E_1", "E_2", "E_3")
        testCase("L0@")
        testCase("L1@E_1", "E_1")
    }

    @Test
    fun `parse with common meaning test`() {
        fun testCase(expected: ComplexMeaning.Common, input: String) {
            assertEquals(expected, ComplexMeaning.parse(input))
        }

        testCase(ComplexMeaning.Common("123"), "C123")
        testCase(ComplexMeaning.Common(""), "C")
        testCase(ComplexMeaning.Common("1"), "C1")
    }

    @Test
    fun `parse with list meaning test`() {
        fun testCase(expected: ComplexMeaning.List, input: String) {
            val actual = ComplexMeaning.parse(input)

            assertEquals(expected, actual)
        }

        testCase(ComplexMeaning.List(arrayOf("1", "2", "3")), "L3@1\n2\n3")
        testCase(ComplexMeaning.List(emptyArray()), "L0@")
        testCase(ComplexMeaning.List(arrayOf("1")), "L1@1")
        testCase(ComplexMeaning.List(arrayOf("1", "", "3")), "L3@1\n\n3")
    }

    @Test
    fun `anyElementStartsWith test`() {
        fun testCase(rawText: String, prefix: String, ignoreCase: Boolean, expectedResult: Boolean) {
            assertEquals(expectedResult, ComplexMeaning.anyElementStartsWith(rawText, prefix, ignoreCase))
        }

        testCase("Cabd", "ab", ignoreCase = true, expectedResult = true)
        testCase("Cabd", "abd", ignoreCase = true, expectedResult = true)
        testCase("Cbbb", "bbb", ignoreCase = true, expectedResult = true)
        testCase("CABDCC", "ab", ignoreCase = true, expectedResult = true)
        testCase("CABDCC", "ab", ignoreCase = false, expectedResult = false)

        testCase("L1@abc", "abc", ignoreCase = true, expectedResult = true)
        testCase("L2@a\nbb", "b", ignoreCase = true, expectedResult = true)
        testCase("L2@uu\naa", "r", ignoreCase = true, expectedResult = false)
        testCase("L0@", "123", ignoreCase = true, expectedResult = false)
        testCase("L1@A", "a", ignoreCase = true, expectedResult = true)
        testCase("L1@A", "a", ignoreCase = false, expectedResult = false)
        testCase("L2@A\nBB", "a", ignoreCase = true, expectedResult = true)
    }

    @Test
    fun `common mergedWith test`() {
        fun testCase(origin: ComplexMeaning.Common, other: ComplexMeaning, expected: ComplexMeaning) {
            assertEquals(origin.mergedWith(other), expected)
        }

        testCase(ComplexMeaning.Common("123"), ComplexMeaning.Common("1"), expected = ComplexMeaning.List("123", "1"))
        testCase(ComplexMeaning.Common("123"), ComplexMeaning.Common("123"), expected = ComplexMeaning.Common("123"))
    }

    @Test
    fun `list mergedWith test`() {
        fun testCase(origin: ComplexMeaning.List, other: ComplexMeaning, expected: ComplexMeaning) {
            assertEquals(origin.mergedWith(other), expected)
        }

        testCase(
            ComplexMeaning.List("1", "2"),
            ComplexMeaning.List("3", "4"),
            expected = ComplexMeaning.List("1", "2", "3", "4")
        )

        testCase(
            ComplexMeaning.List("1", "2"),
            ComplexMeaning.List("2", "3"),
            expected = ComplexMeaning.List("1", "2", "3")
        )

        testCase(
            ComplexMeaning.List("1", "2"),
            ComplexMeaning.List("1", "2"),
            expected = ComplexMeaning.List("1", "2")
        )

        testCase(
            ComplexMeaning.List("1", "2"),
            ComplexMeaning.Common("1"),
            expected = ComplexMeaning.List("1", "2")
        )

        testCase(
            ComplexMeaning.List("1", "2"),
            ComplexMeaning.Common("3"),
            expected = ComplexMeaning.List("1", "2", "3")
        )
    }
}