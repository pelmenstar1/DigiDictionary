package io.github.pelmenstar1.digiDict.common.time

object TimeUtils {
    private val daysInMonthTable = intArrayOf(
        31, // January
        28, // February
        31, // March
        30, // April
        31, // May
        30, // June
        31, // July
        31, // August
        30, // September
        31, // October
        30, // November
        31  // December
    )

    /**
     * Returns amount of days in given [month] (1-based). As the amount is dependent on a year, it's specified too.
     */
    fun getDaysInMonth(year: Int, month: Int): Int {
        if (month == 2) {
            return if (isLeapYear(year)) 29 else 28
        }

        return daysInMonthTable[month - 1]
    }

    /**
     * Determines whether given [year] is leap.
     */
    fun isLeapYear(year: Int): Boolean {
        return (year and 3) == 0 && (year % 100 != 0 || year % 400 == 0)
    }
}