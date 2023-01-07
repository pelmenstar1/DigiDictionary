package io.github.pelmenstar1.digiDict.data

enum class HomeSortType {
    NEWEST,
    OLDEST,
    GREATEST_SCORE,
    LEAST_SCORE,
    ALPHABETIC_BY_EXPRESSION,
    ALPHABETIC_BY_EXPRESSION_INVERSE;

    companion object {
        fun fromOrdinal(ordinal: Int) = when (ordinal) {
            0 -> NEWEST
            1 -> OLDEST
            2 -> GREATEST_SCORE
            3 -> LEAST_SCORE
            4 -> ALPHABETIC_BY_EXPRESSION
            5 -> ALPHABETIC_BY_EXPRESSION_INVERSE
            else -> throw IllegalArgumentException("Invalid ordinal")
        }
    }
}