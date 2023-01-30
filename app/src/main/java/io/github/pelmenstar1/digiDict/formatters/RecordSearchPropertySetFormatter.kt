package io.github.pelmenstar1.digiDict.formatters

import io.github.pelmenstar1.digiDict.search.RecordSearchProperty

/**
 * Provides the methods to format an array of [RecordSearchProperty]
 */
interface RecordSearchPropertySetFormatter {
    /**
     * Returns a string representation of [values].
     *
     * Although the [values] is stored in an array (it's done for performance reasons), it should be conceived as a set,
     * so [values] is expected to have no duplicate elements.
     */
    fun format(values: Array<out RecordSearchProperty>): String
}