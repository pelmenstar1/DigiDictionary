package io.github.pelmenstar1.digiDict

import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.pelmenstar1.digiDict.data.ConciseRecord
import io.github.pelmenstar1.digiDict.ui.home.search.RecordSearchUtil
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@RunWith(AndroidJUnit4::class)
class RecordSearchUtilTests {
    @Test
    fun filterPredicateOnTextRangeTest() {
        fun testCase(text: String, query: String, expected: Boolean, start: Int = 0, end: Int = text.length) {
            val actual = RecordSearchUtil.filterPredicateOnTextRange(text, start, end, query)

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
    }

    @Test
    fun filterPredicate() {
        fun testCase(expr: String, meaning: String, query: String) {
            val record = ConciseRecord(id = 0, expr, meaning, score = 0)

            assertTrue(RecordSearchUtil.filterPredicate(record, query))
        }

        testCase(
            expr = "Wow that's cool",
            meaning = "CMeaning",
            query = "COOL"
        )

        testCase(
            expr = "First word",
            meaning = "CMeaning",
            query = "fir"
        )

        testCase(
            expr = "Expression",
            meaning = "CCommon meaning",
            query = "COMMON"
        )

        testCase(
            expr = "Expression",
            meaning = "CCommon meaning",
            query = "meaning"
        )

        testCase(
            expr = "Expression",
            meaning = "L2@Mean\nKind",
            query = "mEa"
        )

        testCase(
            expr = "Expression",
            meaning = "L2@Mean\nKind",
            query = "kin"
        )
    }

    @Test
    fun prepareQuery() {
        fun testCase(input: String, expected: String) {
            val actual = RecordSearchUtil.prepareQuery(input)

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