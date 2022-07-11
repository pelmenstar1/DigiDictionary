package io.github.pelmenstar1.digiDict.time

import java.util.TimeZone

private val DAYS_IN_MONTH_TABLE = intArrayOf(
    31, /* Jan */
    28, /* Feb */
    31, /* March */
    30, /* Apr */
    31, /* May */
    30, /* June */
    31, /* July */
    31, /* August */
    30, /* September */
    31, /* October */
    30, /* November */
    31, /* December */
)

fun getDaysInMonth(year: Int, month: Int): Int {
    if(month == 2 && isLeapYear(year)) {
        return 29
    }

    return DAYS_IN_MONTH_TABLE[month - 1]
}

fun isLeapYear(year: Int): Boolean {
    return (year and 3) == 0 && (year % 100 != 0 || year % 400 == 0)
}

fun zonedEpochMillis(epochMillis: Long, timeZone: TimeZone): Long {
    return epochMillis + timeZone.getOffset(epochMillis)
}

fun zonedCurrentEpochMillis(timeZone: TimeZone): Long {
    return zonedEpochMillis(System.currentTimeMillis(), timeZone)
}