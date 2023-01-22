package io.github.pelmenstar1.digiDict.ui.home.search

/**
 * Provides the means to get some metadata about current search request, such as "found ranges" that point to the parts of the
 * specified text that caused the record to be found.
 *
 * Methods of this class can be used many times during the search, so it uses approaches that are indented to minimize
 */
interface HomeSearchMetadataProvider {
    /**
     * Changes the current query to a new [value].
     */
    fun onQueryChanged(value: String)

    /**
     * Calculates "found ranges" in given expression value of a record.
     *
     * The returned array must not be saved or used after the next call
     * either to [calculateFoundRangesInExpression] or [calculateFoundRangesInMeaning], it should be a temporary data.
     *
     * Returns the [IntArray] instance whose values means the following:
     * - the first int is the amount of calculated ranges. Note that for performance reasons
     * the returned [IntArray] might contain more ranges than specified in the first value.
     * - the second value is the start of the first range
     * - the third value is the end (exclusive) of the first range
     * - the fourth value is the start of the second range.
     *
     * ... and so on.
     */
    fun calculateFoundRangesInExpression(expr: String): IntArray

    /**
     * Calculates "found ranges" in given meaning value of a record. The meaning must have the format described in ComplexMeaning.
     *
     * The returned array must not be saved or used after the next call
     * either to [calculateFoundRangesInExpression] or [calculateFoundRangesInMeaning], it should be a temporary data.
     *
     * Returns the [IntArray] instance that contains a sequence of sections.
     * The amount of sections is the same as the amount of distinct meanings in complex [meaning].
     *
     * Section format:
     * - the first int in the section is the amount of calculated ranges in this section.
     * - the second value is the start of the first range.
     * Note that any starts and ends in the sections are relative to zero index, not the actual position in [meaning].
     * - the third value is the end of the first range.
     * - the fourth value is the start of the second range.
     *
     * ... and so on
     */
    fun calculateFoundRangesInMeaning(meaning: String): IntArray
}