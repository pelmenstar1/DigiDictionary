package io.github.pelmenstar1.digiDict.common.time

import java.util.*

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
    private const val DAYS_0000_TO_1970 = DAYS_PER_CYCLE * 5L - (30L * 365L + 7L)

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

    private fun calculateYearEstimation(zeroDay: Long): Long {
        return (400L * zeroDay + 591L) / DAYS_PER_CYCLE
    }

    private fun calculateDayOfMonthEstimation(zeroDay: Long, yearEst: Long): Long {
        return zeroDay - (365L * yearEst + yearEst / 4L - yearEst / 100L + yearEst / 400L)
    }

    private fun validateEpochDay(value: Long) {
        if (value < 0) {
            throw IllegalArgumentException("epochDay can't be negative")
        }
    }

    /**
     * Returns a month (1-based) calculated from given epoch day.
     */
    fun getMonthFromEpochDay(epochDay: Long): Int {
        validateEpochDay(epochDay)

        val zeroDay = epochDay + DAYS_0000_TO_1970 - 60
        val yearEst = calculateYearEstimation(zeroDay)
        var doyEst = calculateDayOfMonthEstimation(zeroDay, yearEst)

        if (doyEst < 0) {
            doyEst = calculateDayOfMonthEstimation(zeroDay, yearEst - 1)
        }

        return ((doyEst.toInt() * 5 + 2) / 153 + 2) % 12 + 1
    }

    /**
     * Returns a year calculated from given epoch day
     */
    fun getYearFromEpochDay(epochDay: Long): Int {
        validateEpochDay(epochDay)

        val zeroDay = epochDay + DAYS_0000_TO_1970 - 60

        var yearEst = calculateYearEstimation(zeroDay)
        var doyEst = calculateDayOfMonthEstimation(zeroDay, yearEst)

        if (doyEst < 0) {
            yearEst--
            doyEst = calculateDayOfMonthEstimation(zeroDay, yearEst)
        }

        val marchMonth0 = (doyEst.toInt() * 5 + 2) / 153

        yearEst += (marchMonth0 / 10).toLong()
        return yearEst.toInt()
    }

    /**
     * Converts a date on start of the given year to epoch days.
     */
    fun yearToEpochDay(year: Int): Long {
        if (year < 0) {
            throw IllegalArgumentException("year can't be negative")
        }

        val y = year.toLong()

        var total = 365L * y
        total += (y + 3L) / 4L - (y + 99L) / 100L + (y + 399L) / 400L

        return total - DAYS_0000_TO_1970
    }

    /**
     * Applies time zone offset to UTC epoch seconds and converts the result to epoch days.
     */
    fun toZonedEpochDays(utcEpochSeconds: Long, zone: TimeZone): Long {
        val millis = utcEpochSeconds * 1000

        return (millis + zone.getOffset(millis)) / MILLIS_IN_DAY
    }
}