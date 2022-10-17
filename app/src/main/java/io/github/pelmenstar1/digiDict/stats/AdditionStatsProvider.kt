package io.github.pelmenstar1.digiDict.stats

import androidx.sqlite.db.SupportSQLiteStatement
import io.github.pelmenstar1.digiDict.common.sum
import io.github.pelmenstar1.digiDict.common.time.MILLIS_IN_DAY
import io.github.pelmenstar1.digiDict.common.time.SECONDS_IN_DAY
import io.github.pelmenstar1.digiDict.common.time.TimeUtils
import io.github.pelmenstar1.digiDict.data.AppDatabase
import java.util.*

interface AdditionStatsProvider {
    fun compute(currentEpochSeconds: Long): AdditionStats
}

class DbAdditionStatsProvider(private val appDatabase: AppDatabase) : AdditionStatsProvider {
    private fun SupportSQLiteStatement.bindEpochSecondsAndExecuteToInt(value: Long): Int {
        // Binding index is 1-based.
        bindLong(1, value)

        return simpleQueryForLong().toInt()
    }

    private fun SupportSQLiteStatement.bindDayRangeAndExecuteToInt(startEpochDay: Long): Int {
        val startEpochSeconds = startEpochDay * SECONDS_IN_DAY

        // Binding index is 1-based.
        bindLong(1, startEpochSeconds)
        bindLong(2, startEpochSeconds + SECONDS_IN_DAY)

        return simpleQueryForLong().toInt()
    }

    override fun compute(currentEpochSeconds: Long): AdditionStats {
        val currentEpochDay = currentEpochSeconds / SECONDS_IN_DAY

        val db = appDatabase
        val fromEpochSecondsStatement = db.compileStatement("SELECT COUNT(*) FROM records WHERE dateTime >= ?")

        return fromEpochSecondsStatement.use {
            val dayRangeStatement =
                db.compileStatement("SELECT COUNT(*) FROM records WHERE dateTime >= ? AND dateTime < ?")

            dayRangeStatement.use {
                val last24Hours =
                    fromEpochSecondsStatement.bindEpochSecondsAndExecuteToInt(currentEpochSeconds - SECONDS_IN_DAY)

                // We need to take off one day to make this work as expected
                // because currentEpochDays points to the start of the day.
                val last31DayBound = currentEpochDay - 30

                val perDayForLast31Days = IntArray(31) { i ->
                    dayRangeStatement.bindDayRangeAndExecuteToInt(last31DayBound + i)
                }

                val last7Days = perDayForLast31Days.sum(start = 24)
                val last31Days = perDayForLast31Days.sum()

                val monthStatsEntries = computeMonthAdditionStatsEntries(dayRangeStatement, currentEpochSeconds)

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
        dayRangeStatement: SupportSQLiteStatement,
        currentEpochSeconds: Long
    ): Array<MonthAdditionStats> {
        val calendar = Calendar.getInstance().also { it.timeInMillis = currentEpochSeconds * 1000L }
        val year = calendar[Calendar.YEAR]

        calendar.set(year, 0, 0, 0, 0, 0)

        var epochDay = calendar.timeInMillis / MILLIS_IN_DAY

        return Array(12) { i ->
            val daysInMonth = TimeUtils.getDaysInMonth(year, i + 1)
            val entry = computeMonthAdditionStatsEntry(dayRangeStatement, epochDay, daysInMonth)
            epochDay += daysInMonth

            entry
        }
    }

    private fun computeMonthAdditionStatsEntry(
        dayRangeStatement: SupportSQLiteStatement,
        startEpochDay: Long,
        daysInMonth: Int
    ): MonthAdditionStats {
        var min = Int.MAX_VALUE
        var max = Int.MIN_VALUE
        var total = 0

        for (i in 0 until daysInMonth) {
            val addedRecords = dayRangeStatement.bindDayRangeAndExecuteToInt(startEpochDay + i)

            if (addedRecords < min) {
                min = addedRecords
            }

            if (addedRecords > max) {
                max = addedRecords
            }

            total += addedRecords
        }

        val avg = total.toFloat() / daysInMonth

        return MonthAdditionStats(min, max, avg, total)
    }
}