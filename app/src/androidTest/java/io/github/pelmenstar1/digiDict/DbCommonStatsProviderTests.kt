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

    private fun createRecord(expression: String, epochSeconds: Long): Record {
        return Record(
            id = 0,
            expression = expression,
            rawMeaning = "CMeaning",
            additionalNotes = "AdditionalNotes",
            score = 0,
            epochSeconds = epochSeconds
        )
    }

    @Test
    fun computeTest() = runTest {
        val dao = db.recordDao()
        val commonStatsProvider = DbCommonStatsProvider(db)

        suspend fun testCase(
            createRecords: (epochSeconds: Long) -> Array<Record>,
            expectedCount: Int,
            expectedLast24Hours: Int,
            expectedLast7Days: Int,
            expectedLast31Days: Int
        ) {
            db.reset()

            val currentEpochSeconds = System.currentTimeMillis() / 1000
            val records = createRecords(currentEpochSeconds)

            dao.insertAll(records.asList())

            val (actualCount, additionStats) = commonStatsProvider.compute(currentEpochSeconds)

            assertEquals(expectedCount, actualCount)

            additionStats.also { (actualLast24Hours, actualLast7Days, actualLast31Days) ->
                assertEquals(expectedLast24Hours, actualLast24Hours)
                assertEquals(expectedLast7Days, actualLast7Days)
                assertEquals(expectedLast31Days, actualLast31Days)
            }

        }

        testCase(
            createRecords = { epochSeconds ->
                arrayOf(
                    /* 4 records in range of a day. */
                    createRecord("1", epochSeconds - 1),
                    createRecord("2", epochSeconds - 17 * SECONDS_IN_HOUR),
                    createRecord("3", epochSeconds - 23 * SECONDS_IN_HOUR),

                    // Edge case.
                    createRecord("4", epochSeconds - SECONDS_IN_DAY + 1),

                    /* 7 records in range of a week. */
                    createRecord("5", epochSeconds - 2 * SECONDS_IN_DAY),
                    createRecord("6", epochSeconds - 6 * SECONDS_IN_DAY),

                    // Edge case.
                    createRecord("7", epochSeconds - 7 * SECONDS_IN_DAY + 1),

                    /* 9 records in range of 31 days. */
                    createRecord("8", epochSeconds - 9 * SECONDS_IN_DAY),

                    // Edge case.
                    createRecord("9", epochSeconds - 31 * SECONDS_IN_DAY + 1)
                )
            },
            expectedCount = 9,
            expectedLast24Hours = 4,
            expectedLast7Days = 7,
            expectedLast31Days = 9
        )

        testCase(
            createRecords = { emptyArray() },
            expectedCount = 0,
            expectedLast24Hours = 0,
            expectedLast7Days = 0,
            expectedLast31Days = 0
        )

        testCase(
            createRecords = { epochSeconds ->
                arrayOf(
                    createRecord("1", epochSeconds - 1),
                    createRecord("2", epochSeconds - 2),
                    createRecord("3", epochSeconds - 7 * SECONDS_IN_HOUR),
                    createRecord("4", epochSeconds - 13 * SECONDS_IN_HOUR),
                    createRecord("5", epochSeconds - 9 * SECONDS_IN_HOUR),
                    createRecord("6", epochSeconds - SECONDS_IN_DAY + 1)
                )
            },
            expectedCount = 6,
            expectedLast24Hours = 6,
            expectedLast7Days = 6,
            expectedLast31Days = 6
        )

        testCase(
            createRecords = { epochSeconds ->
                arrayOf(
                    createRecord("1", epochSeconds - 1),

                    createRecord("2", epochSeconds - 2 * SECONDS_IN_DAY),
                    createRecord("3", epochSeconds - 4 * SECONDS_IN_DAY),
                    createRecord("4", epochSeconds - 3 * SECONDS_IN_DAY),
                    createRecord("5", epochSeconds - 6 * SECONDS_IN_DAY)
                )
            },
            expectedCount = 5,
            expectedLast24Hours = 1,
            expectedLast7Days = 5,
            expectedLast31Days = 5
        )

        testCase(
            createRecords = { epochSeconds ->
                arrayOf(
                    createRecord("1", epochSeconds - 2 * SECONDS_IN_DAY),
                    createRecord("2", epochSeconds - 4 * SECONDS_IN_DAY),
                    createRecord("3", epochSeconds - 3 * SECONDS_IN_DAY),
                    createRecord("4", epochSeconds - 6 * SECONDS_IN_DAY)
                )
            },
            expectedCount = 4,
            expectedLast24Hours = 0,
            expectedLast7Days = 4,
            expectedLast31Days = 4
        )

        testCase(
            createRecords = { epochSeconds ->
                arrayOf(
                    createRecord("1", epochSeconds - 8 * SECONDS_IN_DAY),
                    createRecord("2", epochSeconds - 9 * SECONDS_IN_DAY),
                    createRecord("3", epochSeconds - 30 * SECONDS_IN_DAY),
                    createRecord("4", epochSeconds - 17 * SECONDS_IN_DAY)
                )
            },
            expectedCount = 4,
            expectedLast24Hours = 0,
            expectedLast7Days = 0,
            expectedLast31Days = 4
        )
    }
}