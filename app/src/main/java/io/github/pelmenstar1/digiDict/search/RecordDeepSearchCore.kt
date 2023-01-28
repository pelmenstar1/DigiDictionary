package io.github.pelmenstar1.digiDict.search

import io.github.pelmenstar1.digiDict.common.IntList
import io.github.pelmenstar1.digiDict.common.nextLetterOrDigitIndex
import io.github.pelmenstar1.digiDict.common.nextNonLetterOrDigitIndex
import io.github.pelmenstar1.digiDict.data.ComplexMeaning
import io.github.pelmenstar1.digiDict.data.ConciseRecordWithBadges
import org.jetbrains.annotations.TestOnly

object RecordDeepSearchCore : RecordSearchCore {
    override fun filterPredicate(
        record: ConciseRecordWithBadges,
        query: String,
        options: RecordSearchOptions
    ): Boolean {
        val expr = record.expression
        val meaning = record.meaning
        val flags = options.flags

        if ((flags and RecordSearchOptions.FLAG_SEARCH_FOR_EXPRESSION) != 0) {
            if (filterPredicateOnTextRange(expr, 0, expr.length, query)) {
                return true
            }
        }

        if ((flags and RecordSearchOptions.FLAG_SEARCH_FOR_MEANING) != 0) {
            when (meaning[0]) {
                ComplexMeaning.COMMON_MARKER -> {
                    if (filterPredicateOnTextRange(meaning, 1, meaning.length, query)) {
                        return true
                    }
                }
                ComplexMeaning.LIST_MARKER -> {
                    ComplexMeaning.iterateListElementRanges(meaning) { start, end ->
                        if (filterPredicateOnTextRange(meaning, start, end, query)) {
                            return true
                        }
                    }
                }
            }
        }

        return false
    }

    override fun prepareQuery(value: String): String {
        val length = value.length
        val strStart = value.nextLetterOrDigitIndex(0, length)

        // Check if there's even an index to start with.
        // If strStart < 0, means a string doesn't have any letter or digits -- empty string is the most appropriate result in this case.
        if (strStart < 0) {
            return ""
        }

        var strEnd = length - 1
        while (strEnd >= strStart) {
            if (value[strEnd].isLetterOrDigit()) {
                break
            }

            strEnd--
        }

        // strEnd is inclusive, make it exclusive.
        strEnd++

        var strIndex = strStart
        val bufferLength = strEnd - strStart

        // Saves an allocation of CharArray and String.
        if (bufferLength == 0) {
            return ""
        }

        val buffer = CharArray(bufferLength)
        var bufferIndex = 0

        while (strIndex < strEnd) {
            val current = value[strIndex]

            if (current.isLetterOrDigit()) {
                buffer[bufferIndex] = current

                strIndex++
            } else {
                // strIndex can't be -1 because strEnd points to the last letter-or-digit in text.
                // Code execution just can't be here when strIndex is the last index.
                strIndex = value.nextLetterOrDigitIndex(strIndex + 1, strEnd)

                buffer[bufferIndex] = ' '
            }

            // We write to buffer in any case.
            bufferIndex++
        }

        return String(buffer, 0, bufferIndex)
    }

    override fun calculateFoundRanges(
        record: ConciseRecordWithBadges,
        query: String,
        options: RecordSearchOptions,
        list: IntList
    ) {
        val expr = record.expression
        val meaning = record.meaning
        val flags = options.flags

        if ((flags and RecordSearchOptions.FLAG_SEARCH_FOR_EXPRESSION) != 0) {
            calculateFoundRangesOnTextRange(expr, 0, expr.length, query, list)
        } else {
            // mark that there's no ranges
            list.add(0)
        }

        if ((flags and RecordSearchOptions.FLAG_SEARCH_FOR_MEANING) != 0) {
            when (meaning[0]) {
                ComplexMeaning.COMMON_MARKER -> {
                    calculateFoundRangesOnTextRange(meaning, 1, meaning.length, query, list)
                }
                ComplexMeaning.LIST_MARKER -> {
                    ComplexMeaning.iterateListElementRanges(meaning) { start, end ->
                        calculateFoundRangesOnTextRange(meaning, start, end, query, list)
                    }
                }
            }
        } else {
            // mark that none of the meaning parts have any found ranges
            list.addRepeat(0, ComplexMeaning.getMeaningCount(meaning))
        }
    }

    private fun calculateFoundRangesOnTextRange(
        text: String,
        textStart: Int,
        textEnd: Int,
        query: String,
        list: IntList
    ) {
        val listStart = list.size
        val nextListStart = listStart + 1
        list.ensureCapacity(nextListStart)
        list.size = nextListStart

        // Initial value of index should point to a letter textEnd digit, otherwise the first word will be skipped.
        var index = text.nextLetterOrDigitIndex(textStart, textEnd)
        if (index < 0) {
            return
        }

        while (true) {
            if (match(text, index, textEnd, query)) {
                val offsetStart = index - textStart

                list.add(offsetStart) // start
                list.add(offsetStart + query.length) // end
            }

            val nextNonLetterOrDigitIndex = text.nextNonLetterOrDigitIndex(index, textEnd)
            if (nextNonLetterOrDigitIndex < 0) {
                break
            }

            val nextIndex = text.nextLetterOrDigitIndex(nextNonLetterOrDigitIndex, textEnd)
            if (nextIndex < 0) {
                break
            }

            index = nextIndex
        }

        val rangeCount = (list.size - nextListStart) / 2
        list[listStart] = rangeCount
    }

    /**
     * Returns whether text in range `[start; end)` passes a filtering with given [query].
     *
     * The logic assumes that query is prepared ([prepareQuery])
     */
    @TestOnly
    fun filterPredicateOnTextRange(text: String, start: Int, end: Int, query: String): Boolean {
        // Initial value of index should point to a letter or digit, otherwise the first word will be skipped.
        var index = text.nextLetterOrDigitIndex(start, end)
        if (index < 0) {
            return false
        }

        while (true) {
            if (match(text, index, end, query)) {
                return true
            }

            val nextNonLetterOrDigitIndex = text.nextNonLetterOrDigitIndex(index, end)
            if (nextNonLetterOrDigitIndex < 0) {
                return false
            }

            val nextIndex = text.nextLetterOrDigitIndex(nextNonLetterOrDigitIndex, end)
            if (nextIndex < 0) {
                return false
            }

            index = nextIndex
        }
    }

    private fun match(text: String, start: Int, end: Int, query: String): Boolean {
        if (end - start >= query.length) {
            if (crossWordStartsWith(text, start, end, query)) {
                return true
            }
        }

        return false
    }

    private fun crossWordStartsWith(text: String, start: Int, end: Int, query: String): Boolean {
        var textIndex = start
        var queryIndex = 0
        val queryLength = query.length

        while (queryIndex < queryLength) {
            val qc = query[queryIndex]
            val tc = text[textIndex]

            if (tc.isLetterOrDigit()) {
                if (qc != tc) {
                    // The idea of checking both uppercase and lowercase variants of the chars is took from
                    // java.lang.String's regionMatches implementation.
                    val uqc = qc.uppercaseChar()
                    val utc = tc.uppercaseChar()

                    if (!(uqc == utc || utc.lowercaseChar() == uqc.lowercaseChar())) {
                        return false
                    }
                }

                textIndex++
            } else {
                // As query is prepared, a range of non-letter-or-digits in text should correspond to a space in query.
                // Example: query = "Query kkk", text = "Query ... kkk"
                if (qc != ' ') {
                    return false
                }

                textIndex = text.nextLetterOrDigitIndex(textIndex + 1, end)
                if (textIndex < 0) {
                    return false
                }
            }

            // queryIndex should be moved forward in any case.
            queryIndex++
        }

        return true
    }
}