package io.github.pelmenstar1.digiDict.ui.home.search

import io.github.pelmenstar1.digiDict.common.IntList
import io.github.pelmenstar1.digiDict.common.nextLetterOrDigitIndex
import io.github.pelmenstar1.digiDict.common.nextNonLetterOrDigitIndex
import io.github.pelmenstar1.digiDict.data.ComplexMeaning

class HomeSearchMetadataProviderImpl : HomeSearchMetadataProvider {
    private val dataList = IntList(16)
    private var query = ""

    override fun onQueryChanged(value: String) {
        query = RecordSearchUtil.prepareQuery(value)
    }

    override fun calculateFoundRangesInExpression(expr: String): IntArray {
        val list = dataList

        // Set size to 1 in order to make IntList add elements starting from 1 index.
        // It's done to leave a space for the amount of ranges and we don't know it right now.
        list.size = 1
        calculateFoundRangesOnTextRange(expr, 0, expr.length, list)

        val rangeCount = (list.size - 1) / 2
        list[0] = rangeCount

        return list.getUnderlyingArray()
    }

    override fun calculateFoundRangesInMeaning(meaning: String): IntArray {
        val list = dataList

        when (meaning[0]) {
            ComplexMeaning.COMMON_MARKER -> {
                // Set size to 1 in order to make IntList add elements starting from 1 index.
                // It's done to leave a space for the amount of ranges and we don't know it right now.
                list.size = 1

                calculateFoundRangesOnTextRange(meaning, 1, meaning.length, list)

                val rangeCount = (list.size - 1) / 2
                list[0] = rangeCount
            }
            ComplexMeaning.LIST_MARKER -> {
                var index = 0

                ComplexMeaning.iterateListElementRanges(meaning) { start, end ->
                    val startingIndex = index + 1
                    list.size = startingIndex

                    calculateFoundRangesOnTextRange(meaning, start, end, list)
                    val rangeCount = (list.size - startingIndex) / 2
                    list[index] = rangeCount

                    index = list.size
                }
            }
        }

        return list.getUnderlyingArray()
    }

    /**
     * Calculates "found ranges" in given [text] on specified range (from [start] up to [end] exclusive).
     * All the ranges are added to [list]: start, end, start, end and so on. Starts and ends of added ranges are relative
     * to [start] index, so indices start from 0, not [start].
     */
    private fun calculateFoundRangesOnTextRange(text: String, start: Int, end: Int, list: IntList) {
        // Initial value of index should point to a letter or digit, otherwise the first word will be skipped.
        var index = text.nextLetterOrDigitIndex(start, end)
        if (index < 0) {
            return
        }

        while (true) {
            if (RecordSearchUtil.match(text, index, end, query)) {
                val offsetStart = index - start

                list.add(offsetStart) // start
                list.add(offsetStart + query.length) // end
            }

            val nextNonLetterOrDigitIndex = text.nextNonLetterOrDigitIndex(index, end)
            if (nextNonLetterOrDigitIndex < 0) {
                break
            }

            val nextIndex = text.nextLetterOrDigitIndex(nextNonLetterOrDigitIndex, end)
            if (nextIndex < 0) {
                break
            }

            index = nextIndex
        }
    }
}