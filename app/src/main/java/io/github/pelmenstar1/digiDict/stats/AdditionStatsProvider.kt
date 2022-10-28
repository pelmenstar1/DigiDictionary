package io.github.pelmenstar1.digiDict.stats

import android.database.Cursor
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteProgram
import androidx.sqlite.db.SupportSQLiteQuery
import androidx.sqlite.db.SupportSQLiteStatement
import io.github.pelmenstar1.digiDict.common.sum
import io.github.pelmenstar1.digiDict.common.time.SECONDS_IN_DAY
import io.github.pelmenstar1.digiDict.common.time.TimeUtils
import io.github.pelmenstar1.digiDict.data.AppDatabase

interface AdditionStatsProvider {
    fun compute(currentEpochSeconds: Long): AdditionStats
}

class DbAdditionStatsProvider(private val appDatabase: AppDatabase) : AdditionStatsProvider {
    private class FromEpochSecondsAggregateCountQuery : SupportSQLiteQuery {
        private var startEpochSeconds = 0L

        // This query returns an epoch day and corresponding amount of added records during that day
        // starting from given epoch seconds (parameter 1).
        // It's based on a fact that division is truncating: (E + a) / 86400 = E / 86400 if a is in [0; 86400)
        override fun getSql() =
            "SELECT dateTime / 86400, COUNT(dateTime / 86400) FROM records WHERE dateTime >= ? GROUP BY dateTime / 86400 ORDER BY dateTime / 86400 ASC"

        fun bindStartEpochSeconds(value: Long) {
            startEpochSeconds = value
        }

        override fun bindTo(statement: SupportSQLiteProgram) {
            statement.bindLong(1, startEpochSeconds)
        }

        override fun getArgCount() = 1
    }

    private fun SupportSQLiteStatement.bindEpochSecondsAndQueryForInt(value: Long): Int {
        // Binding index is 1-based.
        bindLong(1, value)

        return simpleQueryForLong().toInt()
    }

    private fun SupportSQLiteStatement.bindDayRangeAndQueryForInt(startEpochDay: Long): Int {
        val startEpochSeconds = startEpochDay * SECONDS_IN_DAY

        // Binding index is 1-based.
        bindLong(1, startEpochSeconds)
        bindLong(2, startEpochSeconds + SECONDS_IN_DAY)

        return simpleQueryForLong().toInt()
    }

    private fun compileFromEpochSecondsStatement() =
        appDatabase.compileStatement("SELECT COUNT(*) FROM records WHERE dateTime >= ?")

    private fun compileDayRangeStatement() =
        appDatabase.compileStatement("SELECT COUNT(*) FROM records WHERE dateTime >= ? AND dateTime < ?")

    override fun compute(currentEpochSeconds: Long): AdditionStats {
        val currentEpochDay = currentEpochSeconds / SECONDS_IN_DAY

        val fromEpochSecondsAggregateQuery = FromEpochSecondsAggregateCountQuery()

        return compileFromEpochSecondsStatement().use { fromEpochSecondsStatement ->
            compileDayRangeStatement().use { dayRangeStatement ->
                val last24Hours = fromEpochSecondsStatement.bindEpochSecondsAndQueryForInt(
                    currentEpochSeconds - SECONDS_IN_DAY
                )

                // We need to take off one day to make this work as expected
                // because currentEpochDays points to the start of the day.
                val last31DayBound = currentEpochDay - 30

                // TODO: Probably it can be done in one query to sqlite and some postprocessing
                val perDayForLast31Days = IntArray(31) { i ->
                    dayRangeStatement.bindDayRangeAndQueryForInt(last31DayBound + i)
                }

                val last7Days = perDayForLast31Days.sum(start = 24)
                val last31Days = perDayForLast31Days.sum()

                val monthStatsEntries = computeMonthAdditionStatsEntries(
                    appDatabase,
                    fromEpochSecondsAggregateQuery,
                    currentEpochDay
                )

                AdditionStats(
                    last24Hours,
                    last7Days,
                    last31Days,
                    perDayForLast31Days,
                    monthStatsEntries
                )
            }
        }
    }

    private fun computeMonthAdditionStatsEntries(
        db: RoomDatabase,
        fromEpochSecondsQuery: FromEpochSecondsAggregateCountQuery,
        currentEpochDay: Long
    ): Array<MonthAdditionStats> {
        val year = TimeUtils.getYearFromEpochDay(currentEpochDay)
        val alignedYearEpochDay = TimeUtils.yearToEpochDay(year)

        fromEpochSecondsQuery.bindStartEpochSeconds(alignedYearEpochDay * SECONDS_IN_DAY)

        return db.query(fromEpochSecondsQuery).use { cursor ->
            Array(12) { i ->
                computeMonthAdditionStatsEntry(cursor, year, month = i + 1)
            }
        }
    }

    /**
     * Computes [MonthAdditionStats] instance.
     *
     * @param cursor [Cursor] that should be returned by [FromEpochSecondsAggregateCountQuery] query
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
        var min = Int.MAX_VALUE
        var max = Int.MIN_VALUE
        var total = 0

        var daysIterated = 0

        while (cursor.moveToNext()) {
            val epochDay = cursor.getLong(0)
            val epochDayMonth = TimeUtils.getMonthFromEpochDay(epochDay)

            // If we're iterating next month, we should stop - we've calculated everything we need, there's no
            // more information about specified month.
            if (epochDayMonth != month) {
                // Step back as we're on the 'wrong' territory.
                cursor.moveToPrevious()
                break
            }

            val addedRecords = cursor.getInt(1)

            if (addedRecords < min) {
                min = addedRecords
            }

            if (addedRecords > max) {
                max = addedRecords
            }

            daysIterated++
            total += addedRecords
        }

        val daysInMonth = TimeUtils.getDaysInMonth(year, month)

        // If 0 days were processed, min and max are 0 as well.
        if (daysIterated == 0) {
            min = 0
            max = 0
        } else {
            // FromEpochSecondsAggregateCountQuery doesn't report the days during which no records were added.
            // If we've iterated less days than days in the month, it means that some of the elements
            // are zero, so minimum value of amount of added days during the month is zero.
            if (daysIterated != daysInMonth) {
                min = 0
            }
        }

        val avg = total.toFloat() / daysInMonth

        return MonthAdditionStats(min, max, avg)
    }
}