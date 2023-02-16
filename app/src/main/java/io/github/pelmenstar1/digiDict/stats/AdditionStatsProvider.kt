package io.github.pelmenstar1.digiDict.stats

import android.database.Cursor
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteStatement
import io.github.pelmenstar1.digiDict.common.sum
import io.github.pelmenstar1.digiDict.common.time.SECONDS_IN_DAY
import io.github.pelmenstar1.digiDict.common.time.TimeUtils
import io.github.pelmenstar1.digiDict.data.AppDatabase
import kotlin.math.max
import kotlin.math.min

interface AdditionStatsProvider {
    fun compute(currentEpochSeconds: Long): AdditionStats
}

class DbAdditionStatsProvider(private val appDatabase: AppDatabase) : AdditionStatsProvider {
    private fun SupportSQLiteStatement.bindEpochSecondsAndQueryForInt(value: Long): Int {
        // Binding index is 1-based.
        bindLong(1, value)

        return simpleQueryForLong().toInt()
    }

    private fun buildFromEpochSecondsAggregateCountQuery(startEpochDay: Long): String {
        val startEpochSeconds = startEpochDay * SECONDS_IN_DAY

        return "SELECT dateTime / 86400, COUNT(dateTime / 86400) FROM records WHERE dateTime >= $startEpochSeconds GROUP BY dateTime / 86400 ORDER BY dateTime / 86400 ASC"
    }

    private fun compileFromEpochSecondsStatement() =
        appDatabase.compileStatement("SELECT COUNT(*) FROM records WHERE dateTime >= ?")

    override fun compute(currentEpochSeconds: Long): AdditionStats {
        val currentEpochDay = currentEpochSeconds / SECONDS_IN_DAY

        val last24Hours = compileFromEpochSecondsStatement().use {
            it.bindEpochSecondsAndQueryForInt(currentEpochSeconds - SECONDS_IN_DAY)
        }

        val perDayForLast31Days = computePerDayForLast31Days(currentEpochDay)

        val last7Days = perDayForLast31Days.sum(start = 24)
        val last31Days = last7Days + perDayForLast31Days.sum(end = 24)

        val monthStatsEntries = computeMonthAdditionStatsEntries(appDatabase, currentEpochDay)

        return AdditionStats(
            last24Hours,
            last7Days,
            last31Days,
            perDayForLast31Days,
            monthStatsEntries
        )
    }

    private fun computePerDayForLast31Days(currentEpochDay: Long): IntArray {
        // We need to take off one day to make this work as expected
        // because currentEpochDays points to the start of the day.
        val last31DayBound = currentEpochDay - 30

        return appDatabase.query(buildFromEpochSecondsAggregateCountQuery(last31DayBound), null).use { cursor ->
            val result = IntArray(31)

            while (cursor.moveToNext()) {
                val epochDay = cursor.getLong(0)
                val count = cursor.getInt(1)

                result[(epochDay - last31DayBound).toInt()] = count
            }

            result
        }
    }

    private fun computeMonthAdditionStatsEntries(db: RoomDatabase, currentEpochDay: Long): Array<MonthAdditionStats> {
        val year = TimeUtils.getYearFromEpochDay(currentEpochDay)
        val alignedYearEpochDay = TimeUtils.yearToEpochDay(year)

        return db.query(buildFromEpochSecondsAggregateCountQuery(alignedYearEpochDay), null).use { cursor ->
            Array(12) { i ->
                computeMonthAdditionStatsEntry(cursor, year, month = i + 1)
            }
        }
    }

    /**
     * Computes [MonthAdditionStats] instance.
     *
     * @param cursor [Cursor] that is created via query built by [buildFromEpochSecondsAggregateCountQuery]
     * @param year year of the particular month to calculate the statistics for
     * @param month month index (1-based) of the particular month to calculate the statistics for
     */
    private fun computeMonthAdditionStatsEntry(
        cursor: Cursor,
        year: Int,
        month: Int
    ): MonthAdditionStats {
        // As we don't know exactly any element from cursor now, min and max should contain some impossible values for them
        // to be replaced by real values later. It's an essential detail because if all elements in cursor > 1 and we set
        // min to 0, then it will be calculated wrong.
        var minAdded = Int.MAX_VALUE
        var maxAdded = Int.MIN_VALUE
        var total = 0

        var daysIterated = 0

        while (cursor.moveToNext()) {
            val epochDay = cursor.getLong(0)
            val epochDayMonth = TimeUtils.getMonthFromEpochDay(epochDay)

            // If we're iterating next month, we should stop - we've calculated everything we need, there's no
            // more information about specified month.
            if (epochDayMonth != month) {
                // Step back - we're on the 'wrong' month.
                cursor.moveToPrevious()
                break
            }

            val addedRecords = cursor.getInt(1)

            minAdded = min(minAdded, addedRecords)
            maxAdded = max(maxAdded, addedRecords)

            daysIterated++
            total += addedRecords
        }

        val daysInMonth = TimeUtils.getDaysInMonth(year, month)

        // If 0 days were processed, min and max are 0 as well.
        if (daysIterated == 0) {
            minAdded = 0
            maxAdded = 0
        } else {
            // FromEpochSecondsAggregateCountQuery doesn't report the days during which no records were added.
            // If we've iterated less days than days in the month, it means that some of the elements
            // are zero, so minimum value of amount of added days during the month is zero.
            if (daysIterated != daysInMonth) {
                minAdded = 0
            }
        }

        val avg = total.toFloat() / daysInMonth

        return MonthAdditionStats(minAdded, maxAdded, avg)
    }
}