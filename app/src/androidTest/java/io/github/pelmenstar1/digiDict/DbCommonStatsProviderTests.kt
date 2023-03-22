package io.github.pelmenstar1.digiDict

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.pelmenstar1.digiDict.common.time.SECONDS_IN_DAY
import io.github.pelmenstar1.digiDict.common.time.SECONDS_IN_HOUR
import io.github.pelmenstar1.digiDict.common.time.TimeUtils
import io.github.pelmenstar1.digiDict.data.AppDatabase
import io.github.pelmenstar1.digiDict.data.Record
import io.github.pelmenstar1.digiDict.stats.DbCommonStatsProvider
import io.github.pelmenstar1.digiDict.stats.MonthAdditionStats
import io.github.pelmenstar1.digiDict.utils.AppDatabaseUtils
import io.github.pelmenstar1.digiDict.utils.reset
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.util.*
import kotlin.random.Random
import kotlin.test.assertEquals

@RunWith(AndroidJUnit4::class)
class DbCommonStatsProviderTests {
    private class RecordEmitter {
        private val list = ArrayList<Record>()

        fun record(epochSeconds: Long) {
            list.add(createRecord(list.size.toString(), epochSeconds))
        }

        fun toArray(): Array<Record> = list.toTypedArray()
    }

    private class MonthDataEmitter(private val year: Int) {
        private val records = ArrayList<Record>()

        val expectedMonthStatsEntries = HashMap<Int, MonthAdditionStats>()

        fun month(month: Int, emitRecords: DayRecordEmitter.() -> Unit, expectedMonthStats: MonthAdditionStats) {
            DayRecordEmitter(year, month, records).emitRecords()

            expectedMonthStatsEntries[month] = expectedMonthStats
        }

        fun getRecords(): Array<Record> = records.toTypedArray()
    }

    private class DayRecordEmitter(
        private val year: Int,
        private val month: Int,
        private val outRecords: MutableList<Record>
    ) {
        private val random = Random(month)

        fun day(dayOfMonth: Int, count: Int) {
            val startOfDayEpochSeconds = Calendar.getInstance(TimeZone.getTimeZone("UTC")).let {
                it.clear()
                it.set(year, month - 1, dayOfMonth)

                it.timeInMillis / 1000
            }

            var remainingCount = count
            if (remainingCount > 0) {
                addRecord(startOfDayEpochSeconds + 0)
                remainingCount--
            }

            if (remainingCount > 0) {
                addRecord(startOfDayEpochSeconds + SECONDS_IN_DAY - 1)
                remainingCount--
            }

            repeat(remainingCount) {
                val recordEpochSeconds = startOfDayEpochSeconds + random.nextInt(1, (SECONDS_IN_DAY - 1).toInt())

                addRecord(recordEpochSeconds)
            }
        }

        private fun addRecord(epochSeconds: Long) {
            outRecords.add(createRecord(expression = outRecords.size.toString(), epochSeconds))
        }
    }

    private data class DayCount(val day: Int, val count: Int)
    private class ExpectedPerDay(val elementCount: Int, val dayCounts: Array<out DayCount>) {
        fun assertEquals(actual: IntArray) {
            assertEquals(elementCount, actual.size, "size")

            for (day in 0 until elementCount) {
                val actualDayCount = actual[day]
                val expectedDayCount = dayCounts.find { it.day == day }?.count ?: 0

                assertEquals(expectedDayCount, actualDayCount, "day = $day")
            }
        }
    }

    private lateinit var db: AppDatabase

    @Before
    fun before() {
        val context = ApplicationProvider.getApplicationContext<Context>()

        db = AppDatabaseUtils.createTestDatabase(context)
    }

    @After
    fun after() {
        db.close()
    }

    private fun createRecordsThroughEmitter(
        currentEpochSeconds: Long,
        emitRecords: RecordEmitter.(epochSeconds: Long) -> Unit
    ): Array<Record> {
        return RecordEmitter().apply { emitRecords(currentEpochSeconds) }.toArray()
    }

    @Test
    fun computeTest_simple_onlyTotalAddition() = runTest {
        val dao = db.recordDao()
        val commonStatsProvider = DbCommonStatsProvider(db)

        suspend fun testCase(
            emitRecords: RecordEmitter.(epochSeconds: Long) -> Unit,
            expectedCount: Int,
            expectedLast24Hours: Int,
            expectedLast7Days: Int,
            expectedLast31Days: Int
        ) {
            db.reset()

            val currentEpochSeconds = System.currentTimeMillis() / 1000

            dao.insertAll(createRecordsThroughEmitter(currentEpochSeconds, emitRecords))
            val (actualCount, actualAdditionStats) = commonStatsProvider.compute(currentEpochSeconds)

            assertEquals(expectedCount, actualCount, "count")

            actualAdditionStats.also {
                assertEquals(expectedLast24Hours, it.last24Hours, "last24Hours")
                assertEquals(expectedLast7Days, it.last7Days, "last7Days")
                assertEquals(expectedLast31Days, it.last31Days, "last31Days")
            }
        }

        testCase(
            emitRecords = { epochSeconds ->
                /* 4 records in range of a day. */
                record(epochSeconds - 1) // day 0
                record(epochSeconds - 17 * SECONDS_IN_HOUR) // day 0
                record(epochSeconds - 23 * SECONDS_IN_HOUR) // day 0

                // Edge case.
                record(epochSeconds - SECONDS_IN_DAY + 1) // day 0

                /* 6 records in range of a week. */
                record(epochSeconds - 2 * SECONDS_IN_DAY) // day 2
                record(epochSeconds - 6 * SECONDS_IN_DAY) // day 5

                /* 7 records in range of 31 days. */
                record(epochSeconds - 9 * SECONDS_IN_DAY) // day 8
            },
            expectedCount = 7,
            expectedLast24Hours = 4,
            expectedLast7Days = 6,
            expectedLast31Days = 7,
        )

        testCase(
            emitRecords = { },
            expectedCount = 0,
            expectedLast24Hours = 0,
            expectedLast7Days = 0,
            expectedLast31Days = 0
        )

        testCase(
            emitRecords = { epochSeconds ->
                val epochDay = epochSeconds / SECONDS_IN_DAY

                record((epochDay - 6) * SECONDS_IN_DAY)
                record((epochDay - 30) * SECONDS_IN_DAY)
            },
            expectedCount = 2,
            expectedLast24Hours = 0,
            expectedLast7Days = 1,
            expectedLast31Days = 2
        )

        testCase(
            emitRecords = { epochSeconds ->
                record(epochSeconds - 1)
                record(epochSeconds - 2)
                record(epochSeconds - 7 * SECONDS_IN_HOUR)
                record(epochSeconds - 13 * SECONDS_IN_HOUR)
                record(epochSeconds - 9 * SECONDS_IN_HOUR)
                record(epochSeconds - SECONDS_IN_DAY + 1)
            },
            expectedCount = 6,
            expectedLast24Hours = 6,
            expectedLast7Days = 6,
            expectedLast31Days = 6
        )

        testCase(
            emitRecords = { epochSeconds ->
                record(epochSeconds - 1)
                record(epochSeconds - 2 * SECONDS_IN_DAY)
                record(epochSeconds - 4 * SECONDS_IN_DAY)
                record(epochSeconds - 3 * SECONDS_IN_DAY)
                record(epochSeconds - 6 * SECONDS_IN_DAY)
            },
            expectedCount = 5,
            expectedLast24Hours = 1,
            expectedLast7Days = 5,
            expectedLast31Days = 5
        )

        testCase(
            emitRecords = { epochSeconds ->
                record(epochSeconds - 2 * SECONDS_IN_DAY)
                record(epochSeconds - 4 * SECONDS_IN_DAY)
                record(epochSeconds - 3 * SECONDS_IN_DAY)
                record(epochSeconds - 6 * SECONDS_IN_DAY)
            },
            expectedCount = 4,
            expectedLast24Hours = 0,
            expectedLast7Days = 4,
            expectedLast31Days = 4
        )

        testCase(
            emitRecords = { epochSeconds ->
                record(epochSeconds - 8 * SECONDS_IN_DAY)
                record(epochSeconds - 9 * SECONDS_IN_DAY)
                record(epochSeconds - 30 * SECONDS_IN_DAY)
                record(epochSeconds - 17 * SECONDS_IN_DAY)
            },
            expectedCount = 4,
            expectedLast24Hours = 0,
            expectedLast7Days = 0,
            expectedLast31Days = 4
        )
    }

    @Test
    fun computeTest_perDay() = runTest {
        val dao = db.recordDao()
        val commonStatsProvider = DbCommonStatsProvider(db)

        suspend fun testCase(
            emitRecords: RecordEmitter.(epochSeconds: Long) -> Unit,
            expectedPerDayForLast31Days: ExpectedPerDay
        ) {
            db.reset()

            val currentEpochSeconds = System.currentTimeMillis() / 1000
            dao.insertAll(createRecordsThroughEmitter(currentEpochSeconds, emitRecords))

            val actualAdditionStats = commonStatsProvider.compute(currentEpochSeconds).additionStats

            expectedPerDayForLast31Days.assertEquals(actualAdditionStats.perDayForLast31Days)
        }

        testCase(
            emitRecords = { epochSeconds ->
                record(epochSeconds) // 0 day
                record(epochSeconds) // 0 day
                record(epochSeconds - SECONDS_IN_DAY) // 1 day
                record(epochSeconds - 2 * SECONDS_IN_DAY) // 2 day
            },
            expectedPerDayForLast31Days = ExpectedPerDay(
                elementCount = 31,
                dayCounts = arrayOf(
                    DayCount(day = 30, count = 2),
                    DayCount(day = 29, count = 1),
                    DayCount(day = 28, count = 1)
                )
            )
        )

        testCase(
            emitRecords = { },
            expectedPerDayForLast31Days = ExpectedPerDay(elementCount = 31, dayCounts = emptyArray()),
        )

        testCase(
            emitRecords = { epochSeconds ->
                record(epochSeconds - 8 * SECONDS_IN_DAY) // 7 day
                record(epochSeconds - 8 * SECONDS_IN_DAY) // 7 day
                record(epochSeconds - 8 * SECONDS_IN_DAY) // 7 day
                record(epochSeconds - 9 * SECONDS_IN_DAY) // 8 day
                record(epochSeconds - 11 * SECONDS_IN_DAY) // 10 day
            },
            expectedPerDayForLast31Days = ExpectedPerDay(
                elementCount = 31,
                dayCounts = arrayOf(
                    DayCount(day = 22, count = 3),
                    DayCount(day = 21, count = 1),
                    DayCount(day = 19, count = 1),
                )
            )
        )
    }

    @Test
    fun computeTest_alignedYear() = runTest {
        val dao = db.recordDao()
        val commonStatsProvider = DbCommonStatsProvider(db)

        suspend fun testCase(block: MonthDataEmitter.() -> Unit) {
            db.reset()

            val currentEpochSeconds = System.currentTimeMillis() / 1000
            val currentYear = TimeUtils.getYearFromEpochDay(currentEpochSeconds / SECONDS_IN_DAY)

            // Compute stats for the last seconds of this year
            val epochSecondsForCompute = TimeUtils.yearToEpochDay(currentYear + 1) * SECONDS_IN_DAY - 1

            val emitter = MonthDataEmitter(currentYear)
            val records = emitter.also(block).getRecords()

            dao.insertAll(records)

            val actualAdditionStats = commonStatsProvider.compute(epochSecondsForCompute).additionStats
            val expectedMonthEntries = emitter.expectedMonthStatsEntries

            for (i in 0 until 12) {
                val actualMonthStats = actualAdditionStats.monthStatsEntriesForAlignedYear[i]
                val expectedMonthStats = expectedMonthEntries[i + 1] // key is 1-based month here

                val actualMin = actualMonthStats.min
                val actualMax = actualMonthStats.max
                val actualAvg = actualMonthStats.average

                if (expectedMonthStats != null) {
                    assertEquals(expectedMonthStats.min, actualMin)
                    assertEquals(expectedMonthStats.max, actualMax)
                    assertEquals(expectedMonthStats.average, actualAvg, 0.01f)
                } else {
                    assertEquals(0, actualMin)
                    assertEquals(0, actualMax)
                    assertEquals(0f, actualAvg)
                }
            }
        }

        testCase {
            month(
                month = 1,
                emitRecords = {
                    day(dayOfMonth = 1, count = 10)
                    day(dayOfMonth = 2, count = 5)
                    day(dayOfMonth = 27, count = 1)
                    day(dayOfMonth = 31, count = 2) // January always has 31 days
                },
                expectedMonthStats = MonthAdditionStats(min = 0, max = 10, average = 0.58f)
            )
        }

        testCase {
            month(
                month = 3,
                emitRecords = {
                    // emit records for all 31 days
                    day(dayOfMonth = 1, count = 5)
                    for (i in 0 until 30) {
                        day(dayOfMonth = i + 2, count = 2)
                    }
                },
                expectedMonthStats = MonthAdditionStats(min = 2, max = 5, average = 2.096f)
            )
        }

        testCase {
            month(
                month = 4,
                emitRecords = {
                    day(dayOfMonth = 5, count = 2)
                    day(dayOfMonth = 27, count = 1)
                },
                expectedMonthStats = MonthAdditionStats(min = 0, max = 2, average = 0.1f)
            )
            month(
                month = 5,
                emitRecords = {
                    day(dayOfMonth = 2, count = 50)
                    day(dayOfMonth = 3, count = 3)
                },
                expectedMonthStats = MonthAdditionStats(min = 0, max = 50, average = 1.7096f)
            )

            month(
                month = 11,
                emitRecords = {
                    day(dayOfMonth = 1, count = 7)
                },
                expectedMonthStats = MonthAdditionStats(min = 0, max = 7, average = 0.225f)
            )
        }
    }

    companion object {
        internal fun createRecord(expression: String, epochSeconds: Long): Record {
            return Record(
                id = 0,
                expression = expression,
                meaning = "CMeaning",
                additionalNotes = "AdditionalNotes",
                score = 0,
                epochSeconds = epochSeconds
            )
        }
    }
}