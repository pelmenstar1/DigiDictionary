package io.github.pelmenstar1.digiDict

import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.pelmenstar1.digiDict.data.ComplexMeaning
import io.github.pelmenstar1.digiDict.ui.home.search.HomeSearchMetadataProviderImpl
import io.github.pelmenstar1.digiDict.utils.IntRangeSection
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.test.assertContentEquals

@RunWith(AndroidJUnit4::class)
class HomeSearchMetadataProviderImplTests {
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

    @Test
    fun calculateFoundRangesInExpressionTest() {
        fun testCase(text: String, query: String, expectedRanges: Array<IntRange>) {
            val provider = HomeSearchMetadataProviderImpl()
            provider.onQueryChanged(query)

            val data = provider.calculateFoundRangesInExpression(text)
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
    }

    @Test
    fun calculateFoundRangesInMeaningTest() {
        fun testCase(meaning: String, query: String, expectedSections: Array<IntRangeSection>) {
            val provider = HomeSearchMetadataProviderImpl()
            provider.onQueryChanged(query)

            val sectionList = ArrayList<IntRangeSection>()

            val data = provider.calculateFoundRangesInMeaning(meaning)

            var dataIndex = 0
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
}