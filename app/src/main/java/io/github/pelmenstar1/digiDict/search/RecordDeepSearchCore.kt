package io.github.pelmenstar1.digiDict.search

import io.github.pelmenstar1.digiDict.common.IntList
import io.github.pelmenstar1.digiDict.common.nextLetterOrDigitIndex
import io.github.pelmenstar1.digiDict.common.nextNonLetterOrDigitIndex
import io.github.pelmenstar1.digiDict.data.ComplexMeaning
import io.github.pelmenstar1.digiDict.data.ConciseRecordWithBadges
import org.jetbrains.annotations.TestOnly

/**
 * An implementation of [RecordSearchCore] that provides a more deeper search than simple startsWith.
 *
 * The [RecordDeepSearchCore.calculateFoundRanges] is not thread-safe.
 */
object RecordDeepSearchCore : RecordSearchCore {
    private class MutableIntRange {
        @JvmField
        var start: Int = 0

        @JvmField
        var end: Int = 0
    }

    private val cachedRange = MutableIntRange()

    // Marks that the result has not been computed yet. It does not mean that the result is invalid though.
    private const val RESULT_NOT_COMPUTED = -2

    // The min length of the query to search within in-word boundary
    private const val IN_WORD_SEARCH_MIN_LENGTH = 3

    internal const val QUERY_FLAG_SINGLE_WORD = 1

    override fun filterPredicate(
        record: ConciseRecordWithBadges,
        query: String,
        options: RecordSearchOptions
    ): Boolean {
        val expr = record.expression
        val meaning = record.meaning
        val flags = options.flags

        val queryFlags = computeQueryFlags(query)

        if ((flags and RecordSearchOptions.FLAG_SEARCH_FOR_EXPRESSION) != 0) {
            if (filterPredicateOnTextRange(expr, 0, expr.length, query, queryFlags)) {
                return true
            }
        }

        if ((flags and RecordSearchOptions.FLAG_SEARCH_FOR_MEANING) != 0) {
            when (meaning[0]) {
                ComplexMeaning.COMMON_MARKER -> {
                    if (filterPredicateOnTextRange(meaning, 1, meaning.length, query, queryFlags)) {
                        return true
                    }
                }
                ComplexMeaning.LIST_MARKER -> {
                    ComplexMeaning.iterateListElementRanges(meaning) { start, end ->
                        if (filterPredicateOnTextRange(meaning, start, end, query, queryFlags)) {
                            return true
                        }
                    }
                }
            }
        }

        return false
    }

    override fun normalizeQuery(value: String): String {
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

        // In most cases the query is normalized form and no amends should be made.
        // We allocate the buffer only when we actually need it.
        var buffer: CharArray? = null
        var bufferIndex = 0
        var lastStrLodIndex = -1

        while (strIndex < strEnd) {
            val current = value[strIndex]

            if (current.isLetterOrDigit()) {
                buffer?.set(bufferIndex, current)
                lastStrLodIndex = strIndex

                strIndex++
            } else {
                val nextStrIndex = strIndex + 1

                // newStrIndex can't be -1 because strEnd points to the last letter-or-digit in text.
                // We just can't occur here when strIndex is the last index.
                val newStrIndex = value.nextLetterOrDigitIndex(nextStrIndex, strEnd)

                if (buffer == null) {
                    // Check if the query is not normalized, if so, the buffer should be initialized.
                    // Also check if the lastStrLodIndex is not -1
                    // (in that case the string has no letter-or-digits up till strIndex)
                    if (!(newStrIndex == nextStrIndex && current == ' ') && lastStrLodIndex >= 0) {
                        buffer = CharArray(bufferLength)

                        value.toCharArray(
                            buffer,
                            destinationOffset = 0,
                            startIndex = strStart,
                            endIndex = lastStrLodIndex + 1
                        )
                    }
                }

                buffer?.set(bufferIndex, ' ')
                strIndex = newStrIndex
            }

            // Increase the index regardless of even buffer existence. If we eventually creates the buffer
            // we'll need this variable.
            bufferIndex++
        }

        return if (buffer != null) {
            String(buffer, 0, bufferIndex)
        } else {
            // substring won't allocate if strStart == 0 and strEnd == value.length().
            // So we don't need to check that.
            value.substring(strStart, strEnd)
        }
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

        val queryFlags = computeQueryFlags(query)

        if ((flags and RecordSearchOptions.FLAG_SEARCH_FOR_EXPRESSION) != 0) {
            calculateFoundRangesOnTextRange(expr, 0, expr.length, query, queryFlags, list)
        } else {
            // mark that there's no ranges
            list.add(0)
        }

        if ((flags and RecordSearchOptions.FLAG_SEARCH_FOR_MEANING) != 0) {
            when (meaning[0]) {
                ComplexMeaning.COMMON_MARKER -> {
                    calculateFoundRangesOnTextRange(meaning, 1, meaning.length, query, queryFlags, list)
                }
                ComplexMeaning.LIST_MARKER -> {
                    ComplexMeaning.iterateListElementRanges(meaning) { start, end ->
                        calculateFoundRangesOnTextRange(meaning, start, end, query, queryFlags, list)
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
        queryFlags: Int,
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

        val tempRange = cachedRange
        val queryLength = query.length

        while (true) {
            var nextNonLetterOrDigitIndex = RESULT_NOT_COMPUTED

            if (textEnd - index >= queryLength) {
                val foundTextEnd = interWordGetTextEnd(text, index, textEnd, query)

                if (foundTextEnd >= 0) {
                    list.addRange(index - textStart, foundTextEnd - textStart)
                } else if (useInWordSearch(query, queryFlags)) {
                    nextNonLetterOrDigitIndex = text.nextNonLetterOrDigitIndex(index, textEnd)

                    var wordEnd = nextNonLetterOrDigitIndex
                    if (wordEnd < 0) {
                        wordEnd = textEnd
                    }

                    if (inWordMatch(text, textStart, wordEnd, query, foundRange = tempRange)) {
                        list.addRange(tempRange.start - textStart, tempRange.end - textStart)
                    }
                }
            }

            if (nextNonLetterOrDigitIndex == RESULT_NOT_COMPUTED) {
                nextNonLetterOrDigitIndex = text.nextNonLetterOrDigitIndex(index, textEnd)
            }

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

    private fun IntList.addRange(start: Int, end: Int) {
        add(start)
        add(end)
    }

    /**
     * Returns whether text in range `[start; end)` passes a filtering with given [query].
     *
     * The logic assumes that query is normalized.
     */
    @TestOnly
    fun filterPredicateOnTextRange(text: String, start: Int, end: Int, query: String, queryFlags: Int): Boolean {
        // Initial value of index should point to a letter or digit, otherwise the first word will be skipped.
        var index = text.nextLetterOrDigitIndex(start, end)
        if (index < 0) {
            return false
        }

        val queryLength = query.length

        while (true) {
            var nextNonLetterOrDigitIndex = RESULT_NOT_COMPUTED

            if (end - index >= queryLength) {
                if (interWordGetTextEnd(text, index, end, query) >= 0) {
                    return true
                }

                if (useInWordSearch(query, queryFlags)) {
                    nextNonLetterOrDigitIndex = text.nextNonLetterOrDigitIndex(index, end)

                    var wordEnd = nextNonLetterOrDigitIndex
                    if (wordEnd < 0) {
                        wordEnd = end
                    }

                    if (inWordMatch(text, start, wordEnd, query, foundRange = null)) {
                        return true
                    }
                }
            }

            if (nextNonLetterOrDigitIndex == RESULT_NOT_COMPUTED) {
                nextNonLetterOrDigitIndex = text.nextNonLetterOrDigitIndex(index, end)
            }

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

    private fun inWordMatch(text: String, start: Int, end: Int, query: String, foundRange: MutableIntRange?): Boolean {
        val queryLength = query.length

        for (i in start until end) {
            if (text.regionMatches(i, query, 0, queryLength, ignoreCase = true)) {
                if (foundRange != null) {
                    foundRange.start = i
                    foundRange.end = i + queryLength
                }

                return true
            }
        }

        return false
    }

    /**
     * Finds the index where [query] ends in given [text] on specified range (from [start] up to [end]),
     * assuming that [query] is normalized and [text] is not. This index is not always `start + query.length`. It can differ
     * in case when text is "aaa bb  cc" and query is "bb cc".
     */
    private fun interWordGetTextEnd(text: String, start: Int, end: Int, query: String): Int {
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
                        return -1
                    }
                }

                textIndex++
            } else {
                // As query is normalized, a range of non-letter-or-digits in text should correspond to a space in query.
                // Example: query = "Query kkk", text = "Query ... kkk"
                if (qc != ' ') {
                    return -1
                }

                textIndex = text.nextLetterOrDigitIndex(textIndex + 1, end)
                if (textIndex < 0) {
                    return -1
                }
            }

            // queryIndex should be moved forward in any case.
            queryIndex++
        }

        return textIndex
    }

    // Computes query flags for given value. The query should be normalized
    //
    // The query flags are used to not extract the same information from query too many times.
    @TestOnly
    internal fun computeQueryFlags(query: String): Int {
        var flags = 0

        if (query.nextNonLetterOrDigitIndex(0, query.length) < 0) {
            flags = flags or QUERY_FLAG_SINGLE_WORD
        }

        return flags
    }

    private fun useInWordSearch(query: String, queryFlags: Int): Boolean {
        return query.length >= IN_WORD_SEARCH_MIN_LENGTH && (queryFlags and QUERY_FLAG_SINGLE_WORD) != 0
    }
}