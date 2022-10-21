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

    private const val DAYS_PER_CYCLE = 146097
    const val DAYS_0000_TO_1970 = DAYS_PER_CYCLE * 5L - (30L * 365L + 7L)

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

    private fun calculateDayOfMonthEstimation(zeroDay: Long, yearEst: Long): Long {
        return zeroDay - (365 * yearEst + yearEst / 4 - yearEst / 100 + yearEst / 400)
    }

    /**
     * Returns a month (1-based) calculated from epoch day.
     */
    fun getMonthFromEpochDay(epochDay: Long): Int {
        var zeroDay = epochDay + DAYS_0000_TO_1970 - 60

        if (zeroDay < 0L) {
            val adjustCycles = (zeroDay + 1L) / DAYS_PER_CYCLE - 1L
            zeroDay += -adjustCycles * DAYS_PER_CYCLE
        }

        val yearEst = (400L * zeroDay + 591L) / DAYS_PER_CYCLE

        var doyEst = calculateDayOfMonthEstimation(zeroDay, yearEst)
        if (doyEst < 0) {
            doyEst = calculateDayOfMonthEstimation(zeroDay, yearEst - 1)
        }

        return ((doyEst.toInt() * 5 + 2) / 153 + 2) % 12 + 1
    }
}