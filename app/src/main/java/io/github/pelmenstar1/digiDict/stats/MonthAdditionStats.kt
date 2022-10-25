package io.github.pelmenstar1.digiDict.stats

/**
 * Represents an entry which contains concise addition stats information about a particular month.
 */
data class MonthAdditionStats(
    /**
     * The least amount of added records in a day within the month.
     */
    val min: Int,

    /**
     * The greatest amount of added records in a day within the month.
     */
    val max: Int,

    /**
     * Average amount of added records in a day within the month.
     */
    val average: Float
)