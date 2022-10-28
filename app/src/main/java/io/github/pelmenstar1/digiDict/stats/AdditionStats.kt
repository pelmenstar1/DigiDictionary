package io.github.pelmenstar1.digiDict.stats

import io.github.pelmenstar1.digiDict.common.equalsPattern

class AdditionStats(
    val last24Hours: Int,
    val last7Days: Int,
    val last31Days: Int,
    val perDayForLast31Days: IntArray,
    val monthStatsEntriesForAlignedYear: Array<out MonthAdditionStats>
) {
    override fun equals(other: Any?) = equalsPattern(other) { o ->
        last24Hours == o.last24Hours &&
                last7Days == o.last7Days &&
                last31Days == o.last31Days &&
                perDayForLast31Days.contentEquals(o.perDayForLast31Days) &&
                monthStatsEntriesForAlignedYear.contentEquals(o.monthStatsEntriesForAlignedYear)
    }

    override fun hashCode(): Int {
        var result = last24Hours
        result = 31 * result + last7Days
        result = 31 * result + last31Days
        result = 31 * result + perDayForLast31Days.contentHashCode()
        result = 31 * result + monthStatsEntriesForAlignedYear.contentHashCode()

        return result
    }

    override fun toString(): String {
        return "AdditionStats(last24Hours=$last24Hours, last7Days=$last7Days, last31Days=$last31Days, perDayForLast31Days=${perDayForLast31Days.contentToString()}, perMonthForAlignedYear=${monthStatsEntriesForAlignedYear.contentToString()})"
    }
}