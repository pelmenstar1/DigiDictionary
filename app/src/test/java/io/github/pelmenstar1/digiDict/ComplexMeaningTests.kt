package io.github.pelmenstar1.digiDict

import io.github.pelmenstar1.digiDict.data.ComplexMeaning
import org.junit.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals

class ComplexMeaningTests {
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

        val listSep = ComplexMeaning.LIST_NEW_ELEMENT_SEPARATOR

        testCase("L3@E_1${listSep}E_2${listSep}E_3", "E_1", "E_2", "E_3")
        testCase("L0@")
        testCase("L1@E_1", "E_1")
    }

    @Test
    fun parseWithCommonMeaningTest() {
        fun testCase(expected: ComplexMeaning.Common, input: String) {
            assertEquals(expected, ComplexMeaning.parse(input))
        }

        testCase(ComplexMeaning.Common("123"), "C123")
        testCase(ComplexMeaning.Common(""), "C")
        testCase(ComplexMeaning.Common("1"), "C1")
    }

    @Test
    fun parseWithListMeaningTest() {
        fun testCase(expected: ComplexMeaning.List, input: String) {
            val actual = ComplexMeaning.parse(input)

            assertEquals(expected, actual)
        }

        val listSep = ComplexMeaning.LIST_NEW_ELEMENT_SEPARATOR

        testCase(ComplexMeaning.List(arrayOf("1", "2", "3")), "L3@1${listSep}2${listSep}3")
        testCase(ComplexMeaning.List(emptyArray()), "L0@")
        testCase(ComplexMeaning.List(arrayOf("1")), "L1@1")
        testCase(ComplexMeaning.List(arrayOf("1", "", "3")), "L3@1${listSep}${listSep}3")
    }


    @Test
    fun iterateListElementRangesTest() {
        fun testCase(text: String, expectedRanges: Array<Pair<Int, Int>>) {
            val actualRanges = ArrayList<Pair<Int, Int>>()

            ComplexMeaning.iterateListElementRanges(text) { start, end ->
                actualRanges.add(start to end)
            }

            assertContentEquals(expectedRanges, actualRanges.toTypedArray())
        }

        val listSep = ComplexMeaning.LIST_NEW_ELEMENT_SEPARATOR

        testCase("L1@A", expectedRanges = arrayOf(3 to 4))
        testCase("L0@", expectedRanges = emptyArray())
        testCase("L3@Abcc${listSep}b${listSep}1", expectedRanges = arrayOf(3 to 7, 8 to 9, 10 to 11))
        testCase("L1@", expectedRanges = arrayOf(3 to 3))
        testCase("L2@1${listSep}", expectedRanges = arrayOf(3 to 4, 5 to 5))
    }
}