package io.github.pelmenstar1.digiDict.search

import io.github.pelmenstar1.digiDict.data.ComplexMeaning
import io.github.pelmenstar1.digiDict.data.ConciseRecordWithBadges

/**
 * Provides the means to get some metadata about current search request, such as "found ranges" that point to the parts of the
 * specified text that caused the record to be found.
 *
 * Methods of this class can be used many times during the search, so it uses approaches that are indented to minimize
 */
interface RecordSearchMetadataProvider {
    /**
     * Changes the current query to a new [value].
     */
    fun onQueryChanged(value: String)

    /**
     * Calculates "found ranges" in specified [record].
     *
     * Returns the [IntArray] instance that is divided into sections.
     *
     * Expression section:
     * - the first int is the amount of calculated ranges in expression.
     * - the second value is the start of the first range
     * - the third value is the end (exclusive) of the first range
     * - the fourth value is the start of the second range.
     *
     * ... and so on.
     *
     * Meaning section (after the expression section) is divided into subsections. The amount of subsections equals to
     * amount of discrete meanings in the [ComplexMeaning] of [record].
     *
     * Meaning subsection format:
     * - the first int in the section is the amount of calculated ranges in this section.
     * - the second value is the start of the first range.
     * Note that starts and ends in the sections are relative to zero index, not the actual position in raw meaning string.
     * - the third value is the end of the first range.
     * - the fourth value is the start of the second range.
     *
     * ... and so on
     */
    fun calculateFoundRanges(record: ConciseRecordWithBadges): IntArray
}