package io.github.pelmenstar1.digiDict

import io.github.pelmenstar1.digiDict.ui.home.search.RecordSearchUtil
import io.github.pelmenstar1.digiDict.utils.NULL_CHAR
import org.junit.Test
import kotlin.test.assertEquals

class RecordSearchUtilTests {
    @Test
    fun test() {
        fun testCase(keywords: String, query: String, expected: Boolean) {
            val actualResult = RecordSearchUtil.filterPredicate(keywords, query)

            assertEquals(expected, actualResult)
        }

        testCase(keywords = "expr1 pp1${NULL_CHAR}m m1", query = "p", expected = true)
        testCase(keywords = "xpr1${NULL_CHAR}M", query = "expr1", expected = false)
        testCase(keywords = "expr1 expr2${NULL_CHAR}meaning b", query = "mea", expected = true)
        testCase(keywords = "expr1${NULL_CHAR}meaning", query = "expr", expected = true)
        testCase(keywords = "e${NULL_CHAR}b", query = "b", expected = true)
        testCase(keywords = "exx${NULL_CHAR}m n", query = "n", expected = true)
    }
}