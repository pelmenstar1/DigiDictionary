package io.github.pelmenstar1.digiDict.search

/**
 * Represents an enum of possible properties of record using what a search can be done.
 */
enum class RecordSearchProperty {
    EXPRESSION,
    MEANING;

    companion object {
        fun ofOrdinal(ordinal: Int) = when (ordinal) {
            0 -> EXPRESSION
            1 -> MEANING
            else -> throw IllegalArgumentException("ordinal")
        }
    }
}