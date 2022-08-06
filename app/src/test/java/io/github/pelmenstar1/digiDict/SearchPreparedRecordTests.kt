package io.github.pelmenstar1.digiDict

import io.github.pelmenstar1.digiDict.data.SearchPreparedRecord
import io.github.pelmenstar1.digiDict.utils.NULL_CHAR
import org.junit.Test
import kotlin.test.assertEquals

class SearchPreparedRecordTests {
    @Test
    fun prepareToKeywordsTest() {
        fun testCase(expr: String, meaning: String, expectedKeywords: String) {
            val actualKeywords = SearchPreparedRecord.prepareToKeywords(expr, meaning, needToLower = true)

            assertEquals(expectedKeywords, actualKeywords)
        }

        testCase(expr = "Expression", meaning = "CMeaning", expectedKeywords = "expression${NULL_CHAR}meaning")
        testCase(expr = "Expression", meaning = "C", expectedKeywords = "expression$NULL_CHAR")
        testCase(expr = "Expression", meaning = "L1@Meaning", expectedKeywords = "expression${NULL_CHAR}meaning")
        testCase(expr = "Expr 1 2  ", meaning = "L2@M1\nM2", expectedKeywords = "expr 1 2${NULL_CHAR}m1 m2")
        testCase(expr = "Expr 1 2", meaning = "L3@1\n2\n3", expectedKeywords = "expr 1 2${NULL_CHAR}1 2 3")
    }
}