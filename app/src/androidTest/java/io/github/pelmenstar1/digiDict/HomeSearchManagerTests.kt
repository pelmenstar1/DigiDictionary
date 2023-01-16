package io.github.pelmenstar1.digiDict

import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.pelmenstar1.digiDict.common.containsLetterOrDigit
import io.github.pelmenstar1.digiDict.common.mapToArray
import io.github.pelmenstar1.digiDict.commonTestUtils.toArray
import io.github.pelmenstar1.digiDict.data.ConciseRecordWithBadges
import io.github.pelmenstar1.digiDict.ui.home.search.HomeSearchManager
import io.github.pelmenstar1.digiDict.data.HomeSortType
import io.github.pelmenstar1.digiDict.data.getComparatorForConciseRecordWithBadges
import io.github.pelmenstar1.digiDict.ui.home.search.RecordSearchUtil
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

@RunWith(AndroidJUnit4::class)
class HomeSearchManagerTests {
    private val dataRecords = HomeSearchManagerTestsData.words.mapToArray(::createRecord)

    private fun createRecord(expression: String): ConciseRecordWithBadges {
        return ConciseRecordWithBadges(0, expression, "CMeaning", 0, 0, emptyArray())
    }

    @Test
    fun onSearchRequest_noMutations() {
        fun testCase(queries: Array<String>) {
            val manager = HomeSearchManager()
            manager.currentRecords = dataRecords

            var expectedPrevData: Array<ConciseRecordWithBadges>? = null
            val sortTypes = HomeSortType.values()

            for (query in queries) {
                for (sortType in sortTypes) {
                    val result = manager.onSearchRequest(query, sortType)
                    val actualCurrentData = result.currentData.toArray()
                    val expectedCurrentData = if (query.containsLetterOrDigit()) {
                        dataRecords
                            .filter { RecordSearchUtil.filterPredicate(it, query) }
                            .sortedWith(sortType.getComparatorForConciseRecordWithBadges())
                            .toTypedArray()
                    } else {
                        emptyArray()
                    }

                    assertContentEquals(expectedCurrentData, actualCurrentData)

                    if (expectedPrevData != null) {
                        val actualPrevData = result.previousData

                        assertNotNull(actualPrevData)
                        assertContentEquals(expectedPrevData, actualPrevData.toArray())
                    }

                    expectedPrevData = actualCurrentData
                }
            }
        }

        testCase(
            queries = arrayOf(
                "A",
                "AA",
                "A",
                "AB",
                "",
                "A",
                "AC",
                "ACC"
            )
        )

        testCase(
            queries = arrayOf(
                "",
                "A",
                "abandoned",
                "ab",
                "",
                "ab",
                "o",
                "of",
                "off",
                "o",
                "ok"
            )
        )

        testCase(queries = arrayOf("", ""))
        testCase(queries = arrayOf("a", "able", "a", ""))
    }

    private fun onSearchRequestMutationsHelper(
        oldExpressionsData: Array<String>,
        newExpressionsData: Array<String>,
        searchQuery: String,
        oldExpectedCurrentExpressions: Array<String>,
        newExpectedCurrentExpressions: Array<String>
    ) {
        val manager = HomeSearchManager()
        manager.currentRecords = oldExpressionsData.mapToArray(::createRecord)

        val oldResult = manager.onSearchRequest(searchQuery, HomeSortType.ALPHABETIC_BY_EXPRESSION)
        val oldActualCurrentData = oldResult.currentData.toArray()
        val oldExpectedCurrentData = oldExpectedCurrentExpressions.mapToArray(::createRecord)

        assertEquals(0, oldResult.previousData.size)
        assertContentEquals(oldExpectedCurrentData, oldActualCurrentData)

        manager.currentRecords = newExpressionsData.mapToArray(::createRecord)

        val newResult = manager.onSearchRequest(searchQuery, HomeSortType.ALPHABETIC_BY_EXPRESSION)
        val newActualCurrentData = newResult.currentData.toArray()
        val newExpectedCurrentData = newExpectedCurrentExpressions.mapToArray(::createRecord)

        val newActualPrevData = newResult.previousData.toArray()

        // old current data is new previous data by now.
        @Suppress("UnnecessaryVariable")
        val newExpectedPrevData = oldActualCurrentData

        assertContentEquals(newExpectedCurrentData, newActualCurrentData)
        assertContentEquals(newExpectedPrevData, newActualPrevData)
    }

    @Test
    fun onSearchRequest_added() {
        onSearchRequestMutationsHelper(
            oldExpressionsData = arrayOf("a", "aa", "b", "c"),
            newExpressionsData = arrayOf("a", "aa", "aaa", "b", "c"),
            searchQuery = "a",
            oldExpectedCurrentExpressions = arrayOf("a", "aa"),
            newExpectedCurrentExpressions = arrayOf("a", "aa", "aaa")
        )
    }

    @Test
    fun onSearchRequest_removed() {
        onSearchRequestMutationsHelper(
            oldExpressionsData = arrayOf("b", "bb", "bbb", "c", "a"),
            newExpressionsData = arrayOf("b", "bb", "c", "a"),
            searchQuery = "b",
            oldExpectedCurrentExpressions = arrayOf("b", "bb", "bbb"),
            newExpectedCurrentExpressions = arrayOf("b", "bb")
        )

        onSearchRequestMutationsHelper(
            oldExpressionsData = arrayOf("b", "bb", "bbb", "c", "a"),
            newExpressionsData = arrayOf("c", "a"),
            searchQuery = "b",
            oldExpectedCurrentExpressions = arrayOf("b", "bb", "bbb"),
            newExpectedCurrentExpressions = emptyArray()
        )
    }

    @Test
    fun onSearchRequest_updated() {
        onSearchRequestMutationsHelper(
            oldExpressionsData = arrayOf("c0", "c1", "c2", "b", "a"),
            newExpressionsData = arrayOf("c0", "c1", "c3", "b", "a"),
            searchQuery = "c",
            oldExpectedCurrentExpressions = arrayOf("c0", "c1", "c2"),
            newExpectedCurrentExpressions = arrayOf("c0", "c1", "c3")
        )
    }
}