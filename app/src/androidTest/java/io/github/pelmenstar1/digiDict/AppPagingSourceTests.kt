package io.github.pelmenstar1.digiDict

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import io.github.pelmenstar1.digiDict.common.time.SECONDS_IN_DAY
import io.github.pelmenstar1.digiDict.common.time.SECONDS_IN_HOUR
import io.github.pelmenstar1.digiDict.data.ConciseRecordWithBadges
import io.github.pelmenstar1.digiDict.data.RecordBadgeInfo
import io.github.pelmenstar1.digiDict.data.RecordSortType
import io.github.pelmenstar1.digiDict.data.RecordWithBadges
import io.github.pelmenstar1.digiDict.ui.paging.AppPagingSource
import io.github.pelmenstar1.digiDict.ui.paging.PageItem
import io.github.pelmenstar1.digiDict.ui.record.RecordTextPrecomputeController
import io.github.pelmenstar1.digiDict.utils.AppDatabaseUtils
import io.github.pelmenstar1.digiDict.utils.addRecordWithBadges
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.junit.runner.RunWith
import java.util.*
import kotlin.test.assertContentEquals

@RunWith(AndroidJUnit4::class)
class AppPagingSourceTests {
    object TimeSelector {
        fun epochSeconds(day: Int, seconds: Int): Long = day.toLong() * SECONDS_IN_DAY + seconds
        fun epochSeconds(day: Int, seconds: Long) = epochSeconds(day, seconds.toInt())
    }

    class ConciseRecordWithBadgesEmitter {
        private val records = ArrayList<ConciseRecordWithBadges>()

        fun record(id: Int, time: TimeSelector.() -> Long) {
            records.add(createConciseRecord(id, time))
        }

        fun toArray(): Array<ConciseRecordWithBadges> {
            return records.toTypedArray()
        }
    }

    class ItemEmitter {
        private val items = ArrayList<PageItem>()

        fun record(
            id: Int,
            isBeforeDateMarker: Boolean,
            time: TimeSelector.() -> Long
        ) {
            items.add(PageItem.Record(createConciseRecord(id, time), isBeforeDateMarker, null))
        }

        fun dateMarker(epochDay: Long) {
            items.add(PageItem.DateMarker(epochDay))
        }

        fun toArray(): Array<PageItem> {
            return items.toTypedArray()
        }
    }

    private val context = InstrumentationRegistry.getInstrumentation().context

    private fun computePageResultTestHelper(
        sortType: RecordSortType,
        inputRecords: ConciseRecordWithBadgesEmitter.() -> Unit,
        output: ItemEmitter.() -> Unit,
        timeZoneOffsetSeconds: Int = 0,
        computeRange: IntRange? = null
    ) {
        val db = AppDatabaseUtils.createTestDatabase(context)
        val pagingSource = AppPagingSource(db, sortType, RecordTextPrecomputeController.noOp())

        val prevTimeZone = TimeZone.getDefault()
        TimeZone.setDefault(SimpleTimeZone(timeZoneOffsetSeconds * 1000, "TestZone"))

        try {
            val inputRecordsArray = ConciseRecordWithBadgesEmitter().also(inputRecords).toArray()
            val expectedOutput = ItemEmitter().also(output).toArray()

            runBlocking {
                db.clearAllTables()

                for (conciseRecord in inputRecordsArray) {
                    db.addRecordWithBadges(createRecord(conciseRecord))
                }

                val arrayToCompute = if (computeRange != null) {
                    inputRecordsArray.copyOfRange(computeRange.first, computeRange.last + 1)
                } else {
                    inputRecordsArray
                }

                val actualOutput = pagingSource.computePageResult(arrayToCompute).toTypedArray()

                assertContentEquals(expectedOutput, actualOutput)
            }
        } finally {
            TimeZone.setDefault(prevTimeZone)
        }
    }

    @Test
    fun computePageResultTest_newest() {
        fun testCase(
            inputRecords: ConciseRecordWithBadgesEmitter.() -> Unit,
            output: ItemEmitter.() -> Unit,
            timeZoneOffsetSeconds: Int = 0,
            computeRange: IntRange? = null
        ) {
            computePageResultTestHelper(
                RecordSortType.NEWEST,
                inputRecords,
                output,
                timeZoneOffsetSeconds,
                computeRange
            )
        }

        // If there's no records on input, there should be no items on output either.
        testCase(
            inputRecords = {},
            output = {}
        )

        testCase(
            inputRecords = {
                record(id = 1, time = { epochSeconds(day = 1, seconds = 5) })
            },
            output = {
                dateMarker(epochDay = 1)
                record(id = 1, isBeforeDateMarker = false, time = { epochSeconds(day = 1, seconds = 5) })
            }
        )

        testCase(
            inputRecords = {
                record(id = 1, time = { epochSeconds(day = 1, seconds = 7) })
                record(id = 2, time = { epochSeconds(day = 1, seconds = 6) })
                record(id = 3, time = { epochSeconds(day = 1, seconds = 5) })
            },
            output = {
                dateMarker(epochDay = 1)
                record(id = 1, isBeforeDateMarker = false, time = { epochSeconds(day = 1, seconds = 7) })
                record(id = 2, isBeforeDateMarker = false, time = { epochSeconds(day = 1, seconds = 6) })
                record(id = 3, isBeforeDateMarker = false, time = { epochSeconds(day = 1, seconds = 5) })
            }
        )

        testCase(
            inputRecords = {
                record(id = 1, time = { epochSeconds(day = 3, seconds = 1) })
                record(id = 2, time = { epochSeconds(day = 2, seconds = 1) })
                record(id = 3, time = { epochSeconds(day = 1, seconds = 1) })
            },
            output = {
                dateMarker(epochDay = 3)
                record(id = 1, isBeforeDateMarker = true, time = { epochSeconds(day = 3, seconds = 1) })
                dateMarker(epochDay = 2)
                record(id = 2, isBeforeDateMarker = true, time = { epochSeconds(day = 2, seconds = 1) })
                dateMarker(epochDay = 1)
                record(id = 3, isBeforeDateMarker = false, time = { epochSeconds(day = 1, seconds = 1) })
            }
        )

        testCase(
            inputRecords = {
                record(id = 1, time = { epochSeconds(day = 3, seconds = 1) })
                record(id = 2, time = { epochSeconds(day = 2, seconds = 2) })
                record(id = 3, time = { epochSeconds(day = 2, seconds = 1) })

                record(id = 4, time = { epochSeconds(day = 1, seconds = 1) })
            },
            output = {
                dateMarker(epochDay = 3)
                record(id = 1, isBeforeDateMarker = true, time = { epochSeconds(day = 3, seconds = 1) })
                dateMarker(epochDay = 2)
                record(id = 2, isBeforeDateMarker = false, time = { epochSeconds(day = 2, seconds = 2) })
                record(id = 3, isBeforeDateMarker = true, time = { epochSeconds(day = 2, seconds = 1) })
                dateMarker(epochDay = 1)
                record(id = 4, isBeforeDateMarker = false, time = { epochSeconds(day = 1, seconds = 1) })
            }
        )

        // Check whether the logic handles time zone correctly,
        // especially when it's day N in UTC, but day N + 1 in local time zone.
        testCase(
            inputRecords = {
                record(id = 1, time = { epochSeconds(day = 1, seconds = SECONDS_IN_DAY - 100) })
            },
            output = {
                dateMarker(epochDay = 2)
                record(
                    id = 1,
                    isBeforeDateMarker = false,
                    time = { epochSeconds(day = 1, seconds = SECONDS_IN_DAY - 100) }
                )
            },
            timeZoneOffsetSeconds = SECONDS_IN_HOUR.toInt()
        )

        testCase(
            inputRecords = {
                record(id = 1, time = { epochSeconds(day = 1, seconds = SECONDS_IN_DAY - 1) })
                record(id = 2, time = { epochSeconds(day = 1, seconds = SECONDS_IN_DAY - 1001) })
            },
            output = {
                dateMarker(epochDay = 2)
                record(
                    id = 1,
                    isBeforeDateMarker = true,
                    time = { epochSeconds(day = 1, seconds = SECONDS_IN_DAY - 1) }
                )
                dateMarker(epochDay = 1)
                record(
                    id = 2,
                    isBeforeDateMarker = false,
                    time = { epochSeconds(day = 1, seconds = SECONDS_IN_DAY - 1001) }
                )
            },
            timeZoneOffsetSeconds = 1000
        )

        testCase(
            inputRecords = {
                record(id = 1, time = { epochSeconds(day = 1, seconds = SECONDS_IN_DAY - 1) })
                record(id = 2, time = { epochSeconds(day = 1, seconds = SECONDS_IN_DAY - 2) })
                record(id = 3, time = { epochSeconds(day = 1, seconds = SECONDS_IN_DAY - 1001) })
            },
            output = {
                dateMarker(epochDay = 2)
                record(
                    id = 1,
                    isBeforeDateMarker = false,
                    time = { epochSeconds(day = 1, seconds = SECONDS_IN_DAY - 1) }
                )
                record(
                    id = 2,
                    isBeforeDateMarker = true,
                    time = { epochSeconds(day = 1, seconds = SECONDS_IN_DAY - 2) }
                )
                dateMarker(epochDay = 1)
                record(
                    id = 3,
                    isBeforeDateMarker = false,
                    time = { epochSeconds(day = 1, seconds = SECONDS_IN_DAY - 1001) }
                )
            },
            timeZoneOffsetSeconds = 1000
        )

        testCase(
            inputRecords = {
                record(id = 1, time = { epochSeconds(day = 1, seconds = SECONDS_IN_DAY - 1) })
                record(id = 2, time = { epochSeconds(day = 1, seconds = SECONDS_IN_DAY - 2) })
                record(id = 3, time = { epochSeconds(day = 1, seconds = SECONDS_IN_DAY - 1001) })
            },
            output = {
                dateMarker(epochDay = 2)
                record(
                    id = 1,
                    isBeforeDateMarker = false,
                    time = { epochSeconds(day = 1, seconds = SECONDS_IN_DAY - 1) }
                )
                record(
                    id = 2,
                    isBeforeDateMarker = true,
                    time = { epochSeconds(day = 1, seconds = SECONDS_IN_DAY - 2) }
                )
                dateMarker(epochDay = 1)
                record(
                    id = 3,
                    isBeforeDateMarker = false,
                    time = { epochSeconds(day = 1, seconds = SECONDS_IN_DAY - 1001) }
                )
            },
            timeZoneOffsetSeconds = 1000
        )

        testCase(
            inputRecords = {
                record(id = 1, time = { epochSeconds(day = 2, seconds = SECONDS_IN_DAY - 1) })
                record(id = 2, time = { epochSeconds(day = 2, seconds = SECONDS_IN_DAY - 2) })
                record(id = 3, time = { epochSeconds(day = 2, seconds = SECONDS_IN_DAY - 1001) })
                record(id = 4, time = { epochSeconds(day = 2, seconds = SECONDS_IN_DAY - 1007) })
                record(id = 5, time = { epochSeconds(day = 1, seconds = SECONDS_IN_DAY - 1) })
                record(id = 6, time = { epochSeconds(day = 1, seconds = SECONDS_IN_DAY - 1001) })
            },
            output = {
                dateMarker(epochDay = 3)
                record(
                    id = 1,
                    isBeforeDateMarker = false,
                    time = { epochSeconds(day = 2, seconds = SECONDS_IN_DAY - 1) }
                )
                record(
                    id = 2,
                    isBeforeDateMarker = true,
                    time = { epochSeconds(day = 2, seconds = SECONDS_IN_DAY - 2) }
                )
                dateMarker(epochDay = 2)
                record(
                    id = 3,
                    isBeforeDateMarker = false,
                    time = { epochSeconds(day = 2, seconds = SECONDS_IN_DAY - 1001) }
                )
                record(
                    id = 4,
                    isBeforeDateMarker = false,
                    time = { epochSeconds(day = 2, seconds = SECONDS_IN_DAY - 1007) }
                )
                record(
                    id = 5,
                    isBeforeDateMarker = true,
                    time = { epochSeconds(day = 1, seconds = SECONDS_IN_DAY - 1) }
                )
                dateMarker(epochDay = 1)
                record(
                    id = 6,
                    isBeforeDateMarker = false,
                    time = { epochSeconds(day = 1, seconds = SECONDS_IN_DAY - 1001) }
                )
            },
            timeZoneOffsetSeconds = 1000
        )

        // Check if first date marker is inserted only when neccessary
        testCase(
            inputRecords = {
                record(id = 1, time = { epochSeconds(day = 1, seconds = 3) })
                record(id = 2, time = { epochSeconds(day = 1, seconds = 2) })
                record(id = 3, time = { epochSeconds(day = 1, seconds = 1) })
            },
            output = {
                // No date marker should be inserted, because it's not the first record with epoch day 1.
                record(id = 2, isBeforeDateMarker = false, time = { epochSeconds(day = 1, seconds = 2) })
                record(id = 3, isBeforeDateMarker = false, time = { epochSeconds(day = 1, seconds = 1) })
            },
            computeRange = 1 until 3
        )
    }

    @Test
    fun computePageResultTest_oldest() {
        fun testCase(
            inputRecords: ConciseRecordWithBadgesEmitter.() -> Unit,
            output: ItemEmitter.() -> Unit,
            computeRange: IntRange?
        ) {
            computePageResultTestHelper(RecordSortType.OLDEST, inputRecords, output, computeRange = computeRange)
        }

        // computePageResultTest_newest() already checked some cases that need to be checked.
        // And sort type doesn't affect the result much - only how the first date marker is inserted.
        // Basically, we need to check only that case.
        testCase(
            inputRecords = {
                record(id = 1, time = { epochSeconds(day = 1, seconds = 1) })
                record(id = 2, time = { epochSeconds(day = 1, seconds = 2) })
                record(id = 3, time = { epochSeconds(day = 1, seconds = 3) })
            },
            output = {
                // No date marker should be inserted, because it's not the first record with epoch day 1.
                record(id = 2, isBeforeDateMarker = false, time = { epochSeconds(day = 1, seconds = 2) })
                record(id = 3, isBeforeDateMarker = false, time = { epochSeconds(day = 1, seconds = 3) })
            },
            computeRange = 1 until 3
        )
    }

    companion object {
        private fun createConciseRecord(
            id: Int,
            time: TimeSelector.() -> Long
        ): ConciseRecordWithBadges {
            val expr = "Expression$id"
            val meaning = "CMeaning$id"
            val score = id * 100
            val badges = emptyArray<RecordBadgeInfo>()
            val timeValue = TimeSelector.time()

            return ConciseRecordWithBadges(id, expr, meaning, score, timeValue, badges)
        }

        private fun createRecord(conciseRecord: ConciseRecordWithBadges): RecordWithBadges {
            val id = conciseRecord.id

            return RecordWithBadges(
                id,
                conciseRecord.expression,
                conciseRecord.meaning,
                "Notes$id",
                conciseRecord.score,
                conciseRecord.epochSeconds,
                conciseRecord.badges
            )
        }
    }
}