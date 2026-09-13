package io.github.pelmenstar1.digiDict.common.time

import java.time.LocalDate
import java.time.Year
import java.time.YearMonth
import java.util.TimeZone

object TimeUtils {
    /**
     * Returns amount of days in given [month] (1-based). As the amount is dependent on a year, it's specified too.
     */
    fun getDaysInMonth(year: Int, month: Int): Int {
        return YearMonth.of(year, month).lengthOfMonth()
    }

    /**
     * Determines whether given [year] is leap.
     */
    fun isLeapYear(year: Int): Boolean {
        return Year.isLeap(year.toLong())
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

        return LocalDate.ofEpochDay(epochDay).monthValue
    }

    /**
     * Returns a year calculated from given epoch day
     */
    fun getYearFromEpochDay(epochDay: Long): Int {
        validateEpochDay(epochDay)

        return LocalDate.ofEpochDay(epochDay).year
    }

    /**
     * Converts a date on start of the given year to epoch days.
     */
    fun yearToEpochDay(year: Int): Long {
        if (year < 0) {
            throw IllegalArgumentException("year can't be negative")
        }

        return LocalDate.of(year, 1, 1).toEpochDay()
    }

    /**
     * Applies time zone offset to UTC epoch seconds and converts the result to epoch days.
     */
    fun toZonedEpochDays(utcEpochSeconds: Long, zone: TimeZone): Long {
        val millis = utcEpochSeconds * 1000

        return Math.floorDiv(millis + zone.getOffset(millis), MILLIS_IN_DAY)
    }
}
