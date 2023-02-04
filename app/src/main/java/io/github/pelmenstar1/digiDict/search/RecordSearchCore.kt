package io.github.pelmenstar1.digiDict.search

import io.github.pelmenstar1.digiDict.common.IntList
import io.github.pelmenstar1.digiDict.data.ComplexMeaning
import io.github.pelmenstar1.digiDict.data.ConciseRecordWithBadges

/**
 * Provides a core of searching mechanism.
 */
interface RecordSearchCore {
    /**
     * Determines whether given [record] can be found using specified [query].
     *
     * The [query] must be normalized by [normalizeQuery].
     */
    fun filterPredicate(record: ConciseRecordWithBadges, query: String, options: RecordSearchOptions): Boolean

    /**
     * Transforms the query in almost any way that is desired by a core implementation.
     *
     * The only contract is that if the normalized query is empty, the search shouldn't be done.
     */
    fun normalizeQuery(value: String): String

    /**
     * Calculates the ranges because of what the given [record] was found using [query] - found ranges.
     * These ranges should be added to [list] in special format:
     * - the first int added is length of the ranges in expression of the [record]
     * - the next `n * 2`, where `n` is the previous int, ints
     * describe the ranges in the expression that are placed like: start, end, start, end ...
     * - the next ints describe "found ranges" in meaning of the [record]. Each discrete sub-meaning of [ComplexMeaning] has
     * its own subsection. So the next ints describe these subsections.
     *
     * Meaning subsection format:
     * - the first int in the subsection is the length of the range in this subsection
     * - the next `n * 2`, where `n` is the previous int, ints describe the ranges that are placed like: start, end, start, end ...
     */
    fun calculateFoundRanges(
        record: ConciseRecordWithBadges,
        query: String,
        options: RecordSearchOptions,
        list: IntList
    )
}