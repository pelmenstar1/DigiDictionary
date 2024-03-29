package io.github.pelmenstar1.digiDict

import io.github.pelmenstar1.digiDict.data.ComplexMeaning
import org.junit.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals

class ComplexMeaningTests {
    @Test
    fun createCommonTest() {
        assertEquals(ComplexMeaning.common("123").rawText, "C123")
        assertEquals(ComplexMeaning.common("ABCD").rawText, "CABCD")
        assertEquals(ComplexMeaning.common("").rawText, "C")
    }

    @Test
    fun createListTest() {
        fun testCase(expected: String, vararg elements: String) {
            assertEquals(expected, ComplexMeaning.list(elements).rawText)
        }

        val listSep = ComplexMeaning.LIST_NEW_ELEMENT_SEPARATOR

        testCase("L3@E_1${listSep}E_2${listSep}E_3", "E_1", "E_2", "E_3")
        testCase("L0@")
        testCase("L1@E_1", "E_1")
    }

    @Test
    fun parseWithCommonMeaningTest() {
        fun testCase(expected: ComplexMeaning, input: String) {
            assertEquals(expected, ComplexMeaning.parse(input))
        }

        testCase(expected = ComplexMeaning.common("123"), input = "C123")
        testCase(expected = ComplexMeaning.common(""), input = "C")
        testCase(expected = ComplexMeaning.common("1"), input = "C1")
    }

    @Test
    fun parseWithListMeaningTest() {
        fun testCase(expected: ComplexMeaning, input: String) {
            val actual = ComplexMeaning.parse(input)

            assertEquals(expected, actual)
        }

        val listSep = ComplexMeaning.LIST_NEW_ELEMENT_SEPARATOR

        testCase(expected = ComplexMeaning.list(arrayOf("1", "2", "3")), input = "L3@1${listSep}2${listSep}3")
        testCase(expected = ComplexMeaning.list(emptyArray()), input = "L0@")
        testCase(expected = ComplexMeaning.list(arrayOf("1")), input = "L1@1")
        testCase(expected = ComplexMeaning.list(arrayOf("1", "", "3")), input = "L3@1${listSep}${listSep}3")
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

        testCase(text = "L1@A", expectedRanges = arrayOf(3 to 4))
        testCase(text = "L0@", expectedRanges = emptyArray())
        testCase(text = "L3@Abcc${listSep}b${listSep}1", expectedRanges = arrayOf(3 to 7, 8 to 9, 10 to 11))
        testCase(text = "L1@", expectedRanges = arrayOf(3 to 3))
        testCase(text = "L2@1${listSep}", expectedRanges = arrayOf(3 to 4, 5 to 5))
    }

    @Test
    fun getMeaningCountTest() {
        fun testCase(meaning: String, expectedCount: Int) {
            val actualCount = ComplexMeaning.getMeaningCount(meaning)

            assertEquals(expectedCount, actualCount, "meaning: $meaning")
        }

        val listSep = ComplexMeaning.LIST_NEW_ELEMENT_SEPARATOR

        testCase(meaning = "CMeaning", expectedCount = 1)
        testCase(meaning = "L2@Meaning1${listSep}Meaning2", expectedCount = 2)
        testCase(meaning = "L3@Meaning1${listSep}Meaning2${listSep}Meaning3", expectedCount = 3)
    }

    @Test
    fun recodeListOldFormatToNewTest() {
        fun testCase(input: String, expectedMeaning: String) {
            val actualMeaning = ComplexMeaning.recodeListOldFormatToNew(input)

            assertEquals(expectedMeaning, actualMeaning, "input: $input")
        }

        val listSep = ComplexMeaning.LIST_NEW_ELEMENT_SEPARATOR

        testCase(
            input = "L2@Meaning1\nMeaning2",
            expectedMeaning = "L2@Meaning1${listSep}Meaning2"
        )
        testCase(
            input = "L3@Meaning1\nMeaning2\nMeaning3",
            expectedMeaning = "L3@Meaning1${listSep}Meaning2${listSep}Meaning3"
        )
    }

    @Test
    fun recodeListNewFormatToOldTest() {
        fun testCase(input: String, expectedMeaning: String) {
            val actualMeaning = ComplexMeaning.recodeListNewFormatToOld(input)

            assertEquals(expectedMeaning, actualMeaning, "input: $input")
        }

        val listSep = ComplexMeaning.LIST_NEW_ELEMENT_SEPARATOR

        testCase(
            input = "L2@Meaning1${listSep}Meaning2",
            expectedMeaning = "L2@Meaning1\nMeaning2"
        )
        testCase(
            input = "L3@Meaning1${listSep}Meaning2${listSep}Meaning3",
            expectedMeaning = "L3@Meaning1\nMeaning2\nMeaning3"
        )
    }

    @Test
    fun indexOfListSeparatorOrLengthTest() {
        fun testCase(meaning: String, startIndex: Int, expectedIndex: Int) {
            val actualIndex = ComplexMeaning.indexOfListSeparatorOrLength(meaning, startIndex)

            assertEquals(expectedIndex, actualIndex, "meaning: $meaning startIndex: $startIndex")
        }

        val listSep = ComplexMeaning.LIST_NEW_ELEMENT_SEPARATOR

        testCase(meaning = "123${listSep}4", startIndex = 0, expectedIndex = 3)
        testCase(meaning = "1234", startIndex = 0, expectedIndex = 4)
        testCase(meaning = "1${listSep}2${listSep}", startIndex = 2, expectedIndex = 3)
        testCase(meaning = "1${listSep}2345", startIndex = 2, expectedIndex = 6)
    }

    @Test
    fun isValidTest() {
        fun testCase(meaning: String, expectedResult: Boolean) {
            val actualResult = ComplexMeaning.isValid(meaning)

            assertEquals(expectedResult, actualResult, "meaning: $meaning")
        }

        val listSep = ComplexMeaning.LIST_NEW_ELEMENT_SEPARATOR

        testCase(meaning = "", expectedResult = false)
        testCase(meaning = "C", expectedResult = false)
        testCase(meaning = "CMeaning", expectedResult = true)
        testCase(meaning = "CM", expectedResult = true)
        testCase(meaning = "L", expectedResult = false)
        testCase(meaning = "L0@", expectedResult = false)
        testCase(meaning = "L2@Meaning1", expectedResult = false)
        testCase(meaning = "L1@Meaning1${listSep}Meaning2", expectedResult = false)
        testCase(meaning = "L2@Meaning1${listSep}Meaning2\n3", expectedResult = true)
        testCase(meaning = "L2", expectedResult = false)
        testCase(meaning = "Lgrngr@", expectedResult = false)
        testCase(meaning = "L@", expectedResult = false)
        testCase(meaning = "ajnjgrjgg", expectedResult = false)
        testCase(meaning = "@", expectedResult = false)
    }
}