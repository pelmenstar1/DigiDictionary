package io.github.pelmenstar1.digiDict

import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.pelmenstar1.digiDict.data.ComplexMeaning
import io.github.pelmenstar1.digiDict.data.ConciseRecordWithBadges
import io.github.pelmenstar1.digiDict.search.RecordDeepSearchCore
import io.github.pelmenstar1.digiDict.search.RecordSearchMetadataProviderOnCore
import io.github.pelmenstar1.digiDict.search.RecordSearchOptions
import io.github.pelmenstar1.digiDict.utils.IntRangeSection
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals

@RunWith(AndroidJUnit4::class)
class RecordSearchMetadataProviderOnCoreTests {
    private fun dataToRanges(data: IntArray, dataIndex: Int, rangeCount: Int): Array<IntRange> {
        val endIndex = dataIndex + rangeCount * 2
        var index = dataIndex
        val list = ArrayList<IntRange>()

        while (index < endIndex) {
            list.add(data[index] until data[index + 1])
            index += 2
        }

        return list.toTypedArray()
    }

    private fun createRecord(expr: String, meaning: String): ConciseRecordWithBadges {
        return ConciseRecordWithBadges(0, expr, meaning, 0, 0, emptyArray())
    }

    @Test
    fun calculateFoundRangesInExpressionTest() {
        fun testCase(text: String, query: String, expectedRanges: Array<IntRange>) {
            val provider = RecordSearchMetadataProviderOnCore(RecordDeepSearchCore, query, defaultSearchOptions)

            val data = provider.calculateFoundRanges(createRecord(expr = text, meaning = "C1"))
            val rangeCount = data[0]
            val actualRanges = dataToRanges(data, dataIndex = 1, rangeCount)

            assertContentEquals(expectedRanges, actualRanges)
        }

        testCase(
            text = "ABC AB BB",
            query = "AB",
            expectedRanges = arrayOf(0 until 2, 4 until 6)
        )

        testCase(
            text = "BBB",
            query = "AB",
            expectedRanges = emptyArray()
        )

        testCase(
            text = ";;; AA ... AA ;;;; AAB",
            query = "AA",
            expectedRanges = arrayOf(4 until 6, 11 until 13, 19 until 21)
        )

        testCase(
            text = "aspire",
            query = "as",
            expectedRanges = arrayOf(0 until 2)
        )

        testCase(
            text = "give give",
            query = "giv",
            expectedRanges = arrayOf(0 until 3, 5 until 8)
        )

        testCase(
            text = "get",
            query = "get",
            expectedRanges = arrayOf(0 until 3)
        )

        testCase(
            text = "h",
            query = "h",
            expectedRanges = arrayOf(0 until 1)
        )

        testCase(
            text = "abcd",
            query = "bcd",
            expectedRanges = arrayOf(1 until 4)
        )

        testCase(
            text = "abcd efghe",
            query = "ghe",
            expectedRanges = arrayOf(7 until 10)
        )

        // There should be no ranges as the query length is less than current RecordDeepSearchCore.IN_WORD_SEARCH_MIN_LENGTH
        testCase(
            text = "abcd",
            query = "bc",
            expectedRanges = emptyArray()
        )
    }

    @Test
    fun calculateFoundRangesInExpressionWhenFlagDisabledTest() {
        val options = RecordSearchOptions(RecordSearchOptions.FLAG_SEARCH_FOR_MEANING)
        val provider = RecordSearchMetadataProviderOnCore(RecordDeepSearchCore, query = "AB", options)

        val data = provider.calculateFoundRanges(createRecord(expr = "ABC", meaning = "C1"))
        assertEquals(0, data[0])
    }

    @Test
    fun calculateFoundRangesInMeaningTest() {
        fun testCase(meaning: String, query: String, expectedSections: Array<IntRangeSection>) {
            val provider = RecordSearchMetadataProviderOnCore(RecordDeepSearchCore, query, defaultSearchOptions)
            val sectionList = ArrayList<IntRangeSection>()

            val data = provider.calculateFoundRanges(createRecord(expr = "", meaning))

            var dataIndex = data[0] /* length of expression ranges */ * 2 + 1
            val meaningElementCount = ComplexMeaning.getMeaningCount(meaning)

            for (i in 0 until meaningElementCount) {
                val rangeCount = data[dataIndex]
                val ranges = dataToRanges(data, dataIndex + 1, rangeCount)
                sectionList.add(IntRangeSection(ranges))

                dataIndex += rangeCount * 2 + 1
            }

            val actualSections = sectionList.toTypedArray()

            assertContentEquals(expectedSections, actualSections)
        }

        testCase(
            meaning = "CABC AB BB",
            query = "AB",
            expectedSections = arrayOf(
                IntRangeSection(0 until 2, 4 until 6)
            )
        )

        testCase(
            meaning = "CBBB",
            query = "AB",
            expectedSections = arrayOf(IntRangeSection()) // empty section
        )

        testCase(
            meaning = "C;;; AA ... AA ;;;; AAB",
            query = "AA",
            expectedSections = arrayOf(
                IntRangeSection(4 until 6, 11 until 13, 19 until 21)
            )
        )

        testCase(
            meaning = "Caspire",
            query = "as",
            expectedSections = arrayOf(
                IntRangeSection(0 until 2)
            )
        )

        testCase(
            meaning = "Cgive give",
            query = "giv",
            expectedSections = arrayOf(
                IntRangeSection(0 until 3, 5 until 8)
            )
        )

        testCase(
            meaning = "Cget",
            query = "get",
            expectedSections = arrayOf(
                IntRangeSection(0 until 3)
            )
        )

        testCase(
            meaning = "Ch",
            query = "h",
            expectedSections = arrayOf(
                IntRangeSection(0 until 1)
            )
        )

        testCase(
            meaning = "L3@ABC\nABC\nAB",
            query = "AB",
            expectedSections = arrayOf(
                IntRangeSection(0 until 2),
                IntRangeSection(0 until 2),
                IntRangeSection(0 until 2)
            )
        )

        testCase(
            meaning = "L2@B LL\nA",
            query = "LL",
            expectedSections = arrayOf(
                IntRangeSection(2 until 4),
                IntRangeSection()
            )
        )

        testCase(
            meaning = "L2@B\nget",
            query = "get",
            expectedSections = arrayOf(
                IntRangeSection(),
                IntRangeSection(0 until 3)
            )
        )
    }

    @Test
    fun calculateFoundRangesInCommonMeaningWhenFlagDisabledTest() {
        val options = RecordSearchOptions(RecordSearchOptions.FLAG_SEARCH_FOR_MEANING)
        val provider = RecordSearchMetadataProviderOnCore(RecordDeepSearchCore, query = "AB", options)

        val data = provider.calculateFoundRanges(createRecord(expr = "123", meaning = "CABC"))
        assertEquals(0, data[0])
    }

    @Test
    fun calculateFoundRangesInListMeaningWhenFlagDisabledTest() {
        val options = RecordSearchOptions(RecordSearchOptions.FLAG_SEARCH_FOR_EXPRESSION)
        val provider = RecordSearchMetadataProviderOnCore(RecordDeepSearchCore, query = "AB", options)

        val data = provider.calculateFoundRanges(createRecord(expr = "AB", meaning = "L3@AB\nBC\nCD"))

        assertEquals(1, data[0]) // first is expr ranges length, should be one, as there's one range.
        assertEquals(0, data[1]) // the start of the expr range
        assertEquals(2, data[2]) // the end of the expr range
        assertEquals(0, data[3]) // the length of ranges in the first part of the meaning
        assertEquals(0, data[4]) // the length of ranges in the second part of the meaning
        assertEquals(0, data[5]) // the length of ranges in the third part of the meaning
    }

    companion object {
        private val defaultSearchOptions = RecordSearchOptions(
            RecordSearchOptions.FLAG_SEARCH_FOR_EXPRESSION or RecordSearchOptions.FLAG_SEARCH_FOR_MEANING
        )
    }
}