package io.github.pelmenstar1.digiDict

import io.github.pelmenstar1.digiDict.data.ComplexMeaning
import org.junit.Test
import kotlin.test.assertEquals

class ComplexMeaningCreateTests {
    @Test
    fun createCommonTest() {
        assertEquals(ComplexMeaning.Common("123").rawText, "C123")
        assertEquals(ComplexMeaning.Common("ABCD").rawText, "CABCD")
        assertEquals(ComplexMeaning.Common("").rawText, "C")
    }

    @Test
    fun createListTest() {
        fun testCase(expected: String, vararg elements: String) {
            assertEquals(expected, ComplexMeaning.List(elements).rawText)
        }

        testCase("L3@E_1\nE_2\nE_3", "E_1", "E_2", "E_3")
        testCase("L0@")
        testCase("L1@E_1", "E_1")
    }

    @Test
    fun parseCommonTest() {
        fun testCase(expected: ComplexMeaning.Common, input: String) {
            assertEquals(expected, ComplexMeaning.parse(input))
        }

        testCase(ComplexMeaning.Common("123"), "C123")
        testCase(ComplexMeaning.Common(""), "C")
        testCase(ComplexMeaning.Common("1"), "C1")
    }

    @Test
    fun parseListTest() {
        fun testCase(expected: ComplexMeaning.List, input: String) {
            assertEquals(expected, ComplexMeaning.parse(input))
        }

        testCase(ComplexMeaning.List(arrayOf("1", "2", "3")), "L3@1\n2\n3")
        testCase(ComplexMeaning.List(emptyArray()), "L0@")
        testCase(ComplexMeaning.List(arrayOf("1")), "L1@1")
        testCase(ComplexMeaning.List(arrayOf("1", "", "3")), "L3@1\n\n3")
    }
}