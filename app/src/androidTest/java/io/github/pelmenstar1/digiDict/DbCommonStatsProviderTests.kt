package io.github.pelmenstar1.digiDict

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.pelmenstar1.digiDict.common.time.SECONDS_IN_DAY
import io.github.pelmenstar1.digiDict.common.time.SECONDS_IN_HOUR
import io.github.pelmenstar1.digiDict.data.AppDatabase
import io.github.pelmenstar1.digiDict.data.Record
import io.github.pelmenstar1.digiDict.stats.DbCommonStatsProvider
import io.github.pelmenstar1.digiDict.utils.AppDatabaseUtils
import io.github.pelmenstar1.digiDict.utils.reset
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.test.assertEquals

@RunWith(AndroidJUnit4::class)
class DbCommonStatsProviderTests {
    private class RecordEmitter {
        private val list = ArrayList<Record>()

        fun record(epochSeconds: Long) {
            list.add(
                Record(
                    id = 0,
                    expression = list.size.toString(),
                    meaning = "CMeaning",
                    additionalNotes = "AdditionalNotes",
                    score = 0,
                    epochSeconds = epochSeconds
                )
            )
        }

        fun toArray(): Array<Record> = list.toTypedArray()
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

                /* 7 records in range of a week. */
                record(epochSeconds - 2 * SECONDS_IN_DAY) // day 2
                record(epochSeconds - 6 * SECONDS_IN_DAY) // day 5

                /* 9 records in range of 31 days. */
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
}