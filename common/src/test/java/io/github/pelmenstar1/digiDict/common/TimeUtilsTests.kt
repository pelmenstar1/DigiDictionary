package io.github.pelmenstar1.digiDict.common

import io.github.pelmenstar1.digiDict.common.time.SECONDS_IN_DAY
import io.github.pelmenstar1.digiDict.common.time.TimeUtils
import org.junit.Test
import java.time.LocalDate
import java.util.*
import kotlin.test.assertEquals

class TimeUtilsTests {
    @Test
    fun getMonthFromEpochDayTest() {
        for (epochDay in MIN_EPOCH_DAY..MAX_EPOCH_DAY) {
            val expectedMonth = LocalDate.ofEpochDay(epochDay).monthValue
            val actualMonth = TimeUtils.getMonthFromEpochDay(epochDay)

            assertEquals(expectedMonth, actualMonth)
        }
    }

    @Test
    fun getYearFromEpochDayTest() {
        for (epochDay in MIN_EPOCH_DAY..MAX_EPOCH_DAY) {
            val expectedYear = LocalDate.ofEpochDay(epochDay).year
            val actualYear = TimeUtils.getYearFromEpochDay(epochDay)

            assertEquals(expectedYear, actualYear)
        }
    }

    @Test
    fun yearToEpochDayTest() {
        for (year in MIN_YEAR..MAX_YEAR) {
            val expectedEpochDay = LocalDate.of(year, 1, 1).toEpochDay()
            val actualEpochDay = TimeUtils.yearToEpochDay(year)

            assertEquals(expectedEpochDay, actualEpochDay)
        }
    }

    @Test
    fun toZonedEpochDays() {
        val offsetInSeconds = 3

        // This value is handpicked to check whether the offset is actually added
        // If it is, the epoch days will be 2, otherwise it'll be 1, which is wrong.
        val inputEpochSeconds = 2 * SECONDS_IN_DAY - offsetInSeconds + 1

        val zone = SimpleTimeZone(offsetInSeconds * 1000, "Zone")
        val actualResult = TimeUtils.toZonedEpochDays(inputEpochSeconds, zone)

        assertEquals(2, actualResult)
    }

    companion object {
        private const val MIN_EPOCH_DAY = 0L
        private const val MAX_EPOCH_DAY = 2932896L // 31 December 9999

        private const val MIN_YEAR = 1970
        private const val MAX_YEAR = 9999
    }
}