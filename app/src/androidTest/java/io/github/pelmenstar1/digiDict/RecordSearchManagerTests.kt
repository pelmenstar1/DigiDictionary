package io.github.pelmenstar1.digiDict

import androidx.recyclerview.widget.DiffUtil
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.pelmenstar1.digiDict.common.*
import io.github.pelmenstar1.digiDict.commonTestUtils.Diff
import io.github.pelmenstar1.digiDict.commonTestUtils.toArray
import io.github.pelmenstar1.digiDict.data.ConciseRecordWithBadges
import io.github.pelmenstar1.digiDict.data.HomeSortType
import io.github.pelmenstar1.digiDict.data.getComparatorForConciseRecordWithBadges
import io.github.pelmenstar1.digiDict.search.RecordDeepSearchCore
import io.github.pelmenstar1.digiDict.search.RecordSearchManager
import io.github.pelmenstar1.digiDict.ui.EntityWitIdFilteredArrayDiffCallback
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

@RunWith(AndroidJUnit4::class)
class RecordSearchManagerTests {
    private val realisticRecords = mapExpressionsToRecords(RecordSearchManagerTestsData.words)

    private fun createRecord(id: Int, expression: String): ConciseRecordWithBadges {
        return ConciseRecordWithBadges(id, expression, "CMeaning", 0, 0, emptyArray())
    }

    private fun mapExpressionsToRecords(expressions: Array<out String>): Array<ConciseRecordWithBadges> {
        return expressions.mapToArrayIndexed(::createRecord)
    }

    @Test
    fun onSearchRequest_noMutations() {
        fun testCase(queries: Array<String>) {
            val manager = RecordSearchManager(RecordDeepSearchCore)
            manager.currentRecords = realisticRecords

            var expectedPrevData: Array<ConciseRecordWithBadges>? = null
            val sortTypes = HomeSortType.values()

            for (query in queries) {
                for (sortType in sortTypes) {
                    val result = manager.onSearchRequest(query, sortType)
                    val actualCurrentData = result.currentData.toArray()
                    val expectedCurrentData = if (query.containsLetterOrDigit()) {
                        realisticRecords
                            .filter { RecordDeepSearchCore.filterPredicate(it, query) }
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
        val manager = RecordSearchManager(RecordDeepSearchCore)
        manager.currentRecords = mapExpressionsToRecords(oldExpressionsData)

        val oldResult = manager.onSearchRequest(searchQuery, HomeSortType.ALPHABETIC_BY_EXPRESSION)
        val oldActualCurrentData = oldResult.currentData.toArray()
        val oldExpectedCurrentData = mapExpressionsToRecords(oldExpectedCurrentExpressions)

        assertEquals(0, oldResult.previousData.size)
        assertContentEquals(oldExpectedCurrentData, oldActualCurrentData)

        manager.currentRecords = mapExpressionsToRecords(newExpressionsData)

        val newResult = manager.onSearchRequest(searchQuery, HomeSortType.ALPHABETIC_BY_EXPRESSION)
        val newActualCurrentData = newResult.currentData.toArray()
        val newExpectedCurrentData = mapExpressionsToRecords(newExpectedCurrentExpressions)

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

    // Tests whether out implementation of diff works correctly on more realistic data.
    @Test
    fun diffTest() {
        fun testCase(queries: Array<String>) {
            val searchManager = RecordSearchManager(RecordDeepSearchCore)
            searchManager.currentRecords = realisticRecords

            val diffManager = FilteredArrayDiffManager(filteredArrayDiffCallback)

            var expectedPrevData: Array<ConciseRecordWithBadges>? = null

            for (query in queries) {
                val result = searchManager.onSearchRequest(query, HomeSortType.NEWEST)
                val actualCurrentData = result.currentData
                val actualCurrentDataArray = actualCurrentData.toArray()

                val actualPrevData = result.previousData

                val actualDiffResult = diffManager.calculateDifference(actualPrevData, actualCurrentData)
                val actualRanges = ArrayList<Diff.TypedIntRange>()

                actualDiffResult.dispatchTo(Diff.ListUpdateCallbackToList(actualRanges))

                val expectedDiffResult = DiffUtil.calculateDiff(
                    RecyclerViewDiffCallback(actualPrevData, actualCurrentData), false
                )
                val expectedRanges = ArrayList<Diff.TypedIntRange>()
                expectedDiffResult.dispatchUpdatesTo(Diff.RecyclerViewListUpdateCallbackToList(expectedRanges))

                assertContentEquals(expectedRanges, actualRanges)

                if (expectedPrevData == null) {
                    assertEquals(0, actualPrevData.size)
                } else {
                    assertContentEquals(expectedPrevData, actualPrevData.toArray())
                }

                expectedPrevData = actualCurrentDataArray
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
    }

    private class RecyclerViewDiffCallback(
        private val oldArray: FilteredArray<out ConciseRecordWithBadges>,
        private val newArray: FilteredArray<out ConciseRecordWithBadges>
    ) : DiffUtil.Callback() {
        override fun getOldListSize() = oldArray.size
        override fun getNewListSize() = newArray.size

        override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
            return oldArray[oldItemPosition].id == newArray[newItemPosition].id
        }

        override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
            return oldArray[oldItemPosition] == newArray[newItemPosition]
        }
    }

    companion object {
        val filteredArrayDiffCallback = EntityWitIdFilteredArrayDiffCallback<ConciseRecordWithBadges>()
    }
}