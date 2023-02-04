package io.github.pelmenstar1.digiDict

import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.pelmenstar1.digiDict.data.ConciseRecordWithBadges
import io.github.pelmenstar1.digiDict.search.RecordDeepSearchCore
import io.github.pelmenstar1.digiDict.search.RecordSearchOptions
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.test.assertEquals

@RunWith(AndroidJUnit4::class)
class RecordDeepSearchCoreTests {
    @Test
    fun filterPredicateOnTextRangeTest() {
        fun testCase(text: String, query: String, expected: Boolean, start: Int = 0, end: Int = text.length) {
            val queryFlags = RecordDeepSearchCore.computeQueryFlags(query)
            val actual = RecordDeepSearchCore.filterPredicateOnTextRange(text, start, end, query, queryFlags)

            assertEquals(expected, actual, "text: '$text', query: '$query'")
        }

        testCase(
            text = "ABC kkk   n",
            query = "n",
            expected = true
        )

        testCase(
            text = "BB ABC kkk",
            query = "bb",
            expected = false,
            start = 2
        )

        testCase(
            text = "BBB ABC word",
            query = "word",
            expected = false,
            end = 10
        )

        testCase(
            text = "  AAAB",
            query = "aa",
            expected = true
        )

        testCase(
            text = "Sentence,,, with a lot,,  of punctuation",
            query = "lot",
            expected = true
        )

        testCase(
            text = "Sentence,,, with.,, a lot,,  of punctuation",
            query = "with",
            expected = true
        )

        testCase(
            text = "Sentence,,, with a lot,,  of punctuation,,,",
            query = "punctuation",
            expected = true
        )

        testCase(
            text = "ABC    ",
            query = "d",
            expected = false
        )

        testCase(
            text = "Sentence,,, with a lot,,  of punctuation,,,",
            query = "with a lot",
            expected = true
        )

        testCase(
            text = "Cross .. . word ]] search ll ...",
            query = "cross word search",
            expected = true
        )

        testCase(
            text = "Cross",
            query = "Abc",
            expected = false
        )

        testCase(
            text = "  Cross . word s ...",
            query = "word s",
            expected = true
        )

        testCase(
            text = "  Cross   word search",
            query = "cross word",
            expected = true
        )

        testCase(
            text = " cross a word",
            query = "cross word",
            expected = false
        )

        testCase(
            text = "/// dll lll",
            query = "ll",
            expected = true
        )

        testCase(
            text = "ABCD",
            query = "bcd",
            expected = true
        )

        testCase(
            text = "ABCD CD",
            query = "ABC",
            start = 5,
            expected = false
        )

        // False is expected as the query length is less than current RecordDeepSearchCore.IN_WORD_SEARCH_MIN_LENGTH
        testCase(
            text = "ABCD",
            query = "bc",
            expected = false
        )

        testCase(
            text = "abcd cdef",
            query = "def",
            expected = true
        )

        testCase(
            text = "ABC ADE",
            query = "C DE",
            expected = false
        )
    }

    private fun createSearchOptions(searchForExpression: Boolean, searchForMeaning: Boolean): RecordSearchOptions {
        var flags = 0

        if (searchForExpression) {
            flags = RecordSearchOptions.FLAG_SEARCH_FOR_EXPRESSION
        }

        if (searchForMeaning) {
            flags = flags or RecordSearchOptions.FLAG_SEARCH_FOR_MEANING
        }

        return RecordSearchOptions(flags)
    }

    private fun createRecord(expr: String, meaning: String): ConciseRecordWithBadges {
        return ConciseRecordWithBadges(id = 0, expr, meaning, score = 0, epochSeconds = 0, badges = emptyArray())
    }

    private fun filterPredicateTestHelper(
        expr: String,
        meaning: String,
        query: String,
        expectedResult: Boolean,
        createOptions: () -> RecordSearchOptions
    ) {
        val record = createRecord(expr, meaning)
        val options = createOptions()

        assertEquals(expectedResult, RecordDeepSearchCore.filterPredicate(record, query, options))
    }

    @Test
    fun filterPredicateSearchForExpressionAndMeaningTest() {
        fun testCase(expr: String, meaning: String, query: String, expectedResult: Boolean) {
            filterPredicateTestHelper(expr, meaning, query, expectedResult) {
                createSearchOptions(searchForExpression = true, searchForMeaning = true)
            }
        }

        testCase(
            expr = "Wow that's cool",
            meaning = "CMeaning",
            query = "COOL",
            expectedResult = true
        )

        testCase(
            expr = "First word",
            meaning = "CMeaning",
            query = "fir",
            expectedResult = true
        )

        testCase(
            expr = "Expression",
            meaning = "CCommon meaning",
            query = "COMMON",
            expectedResult = true
        )

        testCase(
            expr = "Expression",
            meaning = "CCommon meaning",
            query = "meaning",
            expectedResult = true
        )

        testCase(
            expr = "Expression",
            meaning = "L2@Mean\nKind",
            query = "mEa",
            expectedResult = true
        )

        testCase(
            expr = "Expression",
            meaning = "L2@Mean\nKind",
            query = "kin",
            expectedResult = true
        )
    }

    @Test
    fun filterPredicateSearchForExpressionTest() {
        fun testCase(expr: String, meaning: String, query: String, expectedResult: Boolean) {
            filterPredicateTestHelper(expr, meaning, query, expectedResult) {
                createSearchOptions(searchForExpression = true, searchForMeaning = false)
            }
        }

        testCase(
            expr = "google",
            meaning = "CABC",
            query = "goog",
            expectedResult = true
        )

        testCase(
            expr = "ABC",
            meaning = "Cgoogle",
            query = "google",
            expectedResult = false
        )

        testCase(
            expr = "give",
            meaning = "L2@A\nB",
            query = "A",
            expectedResult = false
        )
    }

    @Test
    fun filterPredicateSearchForMeaningTest() {
        fun testCase(expr: String, meaning: String, query: String, expectedResult: Boolean) {
            filterPredicateTestHelper(expr, meaning, query, expectedResult) {
                createSearchOptions(searchForExpression = false, searchForMeaning = true)
            }
        }

        testCase(
            expr = "ABC",
            meaning = "Cgoogle",
            query = "goog",
            expectedResult = true
        )

        testCase(
            expr = "ABC",
            meaning = "Cgoogle",
            query = "ABC",
            expectedResult = false
        )

        testCase(
            expr = "A",
            meaning = "L2@C\nB",
            query = "A",
            expectedResult = false
        )
    }

    @Test
    fun prepareQueryTest() {
        fun testCase(input: String, expected: String) {
            val actual = RecordDeepSearchCore.prepareQuery(input)

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
        testCase(input = "  A ", expected = "A")
        testCase(input = "  ABC ABC ABC    ", expected = "ABC ABC ABC")
    }

    @Test
    fun computeQueryFlagsTest() {
        fun testCase(query: String, expectedFlags: Int) {
            val actualFlags = RecordDeepSearchCore.computeQueryFlags(RecordDeepSearchCore.prepareQuery(query))

            assertEquals(expectedFlags, actualFlags)
        }

        testCase(query = "a", expectedFlags = RecordDeepSearchCore.QUERY_FLAG_SINGLE_WORD)
        testCase(query = "abc", expectedFlags = RecordDeepSearchCore.QUERY_FLAG_SINGLE_WORD)
        testCase(query = "abc de", expectedFlags = 0)
    }
}