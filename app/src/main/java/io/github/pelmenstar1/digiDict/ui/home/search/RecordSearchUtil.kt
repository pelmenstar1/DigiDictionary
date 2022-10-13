package io.github.pelmenstar1.digiDict.ui.home.search

import io.github.pelmenstar1.digiDict.common.FilteredArray
import io.github.pelmenstar1.digiDict.common.filterFast
import io.github.pelmenstar1.digiDict.common.nextLetterOrDigitIndex
import io.github.pelmenstar1.digiDict.common.nextNonLetterOrDigitIndex
import io.github.pelmenstar1.digiDict.data.ComplexMeaning
import io.github.pelmenstar1.digiDict.data.ConciseRecord
import io.github.pelmenstar1.digiDict.data.ConciseRecordWithBadges
import org.jetbrains.annotations.TestOnly

object RecordSearchUtil {
    /**
     * Filters given [records] array using given [query].
     */
    fun filter(records: Array<out ConciseRecordWithBadges>, query: String): FilteredArray<ConciseRecordWithBadges> {
        val preparedQuery = prepareQuery(query)

        return records.filterFast {
            filterPredicate(it, preparedQuery)
        }
    }

    @TestOnly
    fun filterPredicate(record: ConciseRecord, query: String): Boolean {
        val expr = record.expression
        val meaning = record.meaning

        if (filterPredicateOnTextRange(expr, 0, expr.length, query)) {
            return true
        }

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

        return false
    }

    /**
     * Returns whether text in range `[start; end)` passes a filtering with given [query].
     *
     * The logic assumes that query is prepared ([prepareQuery])
     */
    @TestOnly
    fun filterPredicateOnTextRange(text: String, start: Int, end: Int, query: String): Boolean {
        val queryLength = query.length

        // Initial value of index should point to a letter or digit, otherwise the first word will be skipped.
        var index = text.nextLetterOrDigitIndex(start, end)
        if (index < 0) {
            return false
        }

        while (true) {
            val regionLength = end - index

            // crossWordStartsWith expects that a region to check has the same or bigger length than query.
            if (regionLength >= queryLength) {
                if (crossWordStartsWith(text, index, end, query)) {
                    return true
                }
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

    /**
     * Returns processed subsequence of receiver [String] in range `[start; end)`.
     * The main idea of the 'processing' is to leave only meaningful parts of the sequence joined with spaces.
     * Example: " .. ; . AA  BB     CC DD ;;;" becomes "AA BB CC DD"
     */
    fun prepareQuery(text: String): String {
        val length = text.length
        val strStart = text.nextLetterOrDigitIndex(0, length)

        // Check if there's even an index to start with.
        // If strStart < 0, means a string doesn't have any letter or digits -- empty string is the most appropriate result in this case.
        if (strStart < 0) {
            return ""
        }

        var strEnd = length - 1
        while (strEnd >= strStart) {
            if (text[strEnd].isLetterOrDigit()) {
                break
            }

            strEnd--
        }

        // strEnd is inclusive, make it exclusive.
        strEnd++

        var strIndex = strStart
        val bufferLength = strEnd - strStart

        // Saves an allocation of a CharArray and String.
        if (bufferLength == 0) {
            return ""
        }

        val buffer = CharArray(bufferLength)
        var bufferIndex = 0

        while (strIndex < strEnd) {
            val current = text[strIndex]

            if (current.isLetterOrDigit()) {
                buffer[bufferIndex] = current

                strIndex++
            } else {
                // strIndex can't be -1 because strEnd points to the last letter-or-digit in text.
                // Code execution just can't be here when strIndex is the last index.
                strIndex = text.nextLetterOrDigitIndex(strIndex + 1, strEnd)

                buffer[bufferIndex] = ' '
            }

            // We write to buffer in any case.
            bufferIndex++
        }

        return String(buffer, 0, bufferIndex)
    }
}