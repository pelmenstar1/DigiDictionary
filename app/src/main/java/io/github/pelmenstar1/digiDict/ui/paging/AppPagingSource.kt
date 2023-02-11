package io.github.pelmenstar1.digiDict.ui.paging

import android.util.Log
import androidx.paging.PagingSource
import androidx.paging.PagingState
import androidx.room.InvalidationTracker
import androidx.room.withTransaction
import io.github.pelmenstar1.digiDict.common.time.EpochSecondsRange
import io.github.pelmenstar1.digiDict.common.time.SECONDS_IN_DAY
import io.github.pelmenstar1.digiDict.common.time.TimeUtils
import io.github.pelmenstar1.digiDict.data.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.*

/**
 * [PagingSource] implementation that shows the records info, events, date markers.
 *
 * @param appDatabase database instance
 * @param sortType determines the way to sort the records
 * @param getTimeRangeLambda suspend lambda that returns the time range in which the paging source will work.
 * If there's no limit on time, it should be null. The lambda is called only once.
 */
class AppPagingSource(
    private val appDatabase: AppDatabase,
    private val sortType: RecordSortType,
    private val getTimeRangeLambda: (suspend () -> EpochSecondsRange)? = null
) : PagingSource<Int, PageItem>() {
    private val observer = object : InvalidationTracker.Observer(TABLES) {
        override fun onInvalidated(tables: MutableSet<String>) {
            invalidate()
        }
    }

    private val mutex = Mutex()

    private var itemCount = -1

    @Volatile
    private var timeRange: EpochSecondsRange? = null

    private val recordDao = appDatabase.recordDao()

    init {
        // Both startEpochSeconds and endEpochSeconds should be positive, and startEpochSeconds should be
        // less than or equal to endEpochSeconds. That's the range the logic expects.

        val invalidationTracker = appDatabase.invalidationTracker
        invalidationTracker.addObserver(observer)

        registerInvalidatedCallback {
            invalidationTracker.removeObserver(observer)
        }
    }

    private suspend fun getTimeRange(): EpochSecondsRange {
        val lambda = getTimeRangeLambda ?: return EpochSecondsRange.ALL_TIME_SPAN

        return mutex.withLock {
            var range = timeRange

            if (range == null) {
                range = lambda()
                timeRange = range
            }

            range
        }
    }

    private fun queryItemCount(timeRange: EpochSecondsRange): Int {
        val (startTime, endTime) = timeRange

        val statement = if (startTime == 0L && endTime == Long.MAX_VALUE) {
            appDatabase.compileCountStatement()
        } else {
            appDatabase.compileTimeRangedCountStatement().also {
                it.bindToTimeRangedCountStatement(startTime, endTime)
            }
        }

        return statement.use {
            it.simpleQueryForLong().toInt()
        }
    }

    override fun getRefreshKey(state: PagingState<Int, PageItem>): Int? {
        return when (val anchorPosition = state.anchorPosition) {
            null -> null
            else -> maxOf(0, anchorPosition - (state.config.initialLoadSize / 2))
        }
    }

    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, PageItem> {
        return try {
            appDatabase.withTransaction {
                val timeRange = getTimeRange()

                var count = itemCount
                if (count < 0) {
                    count = queryItemCount(timeRange)
                    itemCount = count
                }

                loadInternal(params, count, timeRange)
            }
        } catch (e: Exception) {
            Log.e(TAG, "during loading", e)

            LoadResult.Error(e)
        }
    }

    private suspend fun loadInternal(
        params: LoadParams<Int>,
        count: Int,
        timeRange: EpochSecondsRange
    ): LoadResult.Page<Int, PageItem> {
        val key = params.key ?: 0
        val loadSize = params.loadSize

        val limit = if (params is LoadParams.Prepend) {
            minOf(key, loadSize)
        } else {
            loadSize
        }

        val offset = when (params) {
            is LoadParams.Prepend -> maxOf(0, key - loadSize)
            is LoadParams.Append -> key
            is LoadParams.Refresh ->
                if (key >= count) {
                    maxOf(0, count - loadSize)
                } else {
                    key
                }
        }

        val recordData = appDatabase.getConciseRecordsWithBadgesForAppPagingSource(
            limit, offset,
            sortType,
            timeRange.start, timeRange.endInclusive
        )

        val recordDataSize = recordData.size

        // For now, the only purpose of computePageResult is to add date markers where neccessary.
        // Doing it when sorting is not related to dates has no sense and very strange.
        //
        // As computePageResult has a single purpose, no flags are added to control the transformation for now.
        val result = if (sortType == RecordSortType.NEWEST || sortType == RecordSortType.OLDEST) {
            computePageResult(recordData, sortType)
        } else {
            recordData.map { PageItem.Record(it, isBeforeDateMarker = false) }
        }

        val nextPosToLoad = offset + recordDataSize

        return LoadResult.Page(
            result,
            prevKey = if (offset <= 0 || recordDataSize == 0) null else offset,
            nextKey = if (recordDataSize == 0 || recordDataSize < limit || nextPosToLoad >= count) {
                null
            } else {
                nextPosToLoad
            }
        )
    }

    private suspend fun computePageResult(
        recordData: Array<out ConciseRecordWithBadges>,
        sortType: RecordSortType
    ): List<PageItem> {
        val dataSize = recordData.size

        return if (dataSize > 0) {
            val zone = TimeZone.getDefault()

            val result = ArrayList<PageItem>((dataSize * 3) / 2)

            val firstRecord = recordData[0]

            val firstRecordEpochSeconds = firstRecord.epochSeconds
            val firstRecordUtcEpochDay = firstRecordEpochSeconds / SECONDS_IN_DAY
            var currentSectionLocalEpochDay = TimeUtils.toZonedEpochDays(firstRecordEpochSeconds, zone)

            val firstRecordIdWithEpochDay = if (sortType == RecordSortType.NEWEST) {
                recordDao.getFirstRecordIdWithEpochDayOrderByEpochDayDesc(firstRecordUtcEpochDay)
            } else {
                recordDao.getFirstRecordIdWithEpochDayOrderByEpochDayAsc(firstRecordUtcEpochDay)
            }

            if (firstRecordIdWithEpochDay == firstRecord.id) {
                result.add(PageItem.DateMarker(currentSectionLocalEpochDay))
            }

            var isFirstRecordBeforeDateMarker = false
            var secondRecordLocalEpochDay = -1L

            if (dataSize > 1) {
                val secondRecordUtcEpochSeconds = recordData[1].epochSeconds
                val secondRecordUtcEpochDay = secondRecordUtcEpochSeconds / SECONDS_IN_DAY

                secondRecordLocalEpochDay = TimeUtils.toZonedEpochDays(secondRecordUtcEpochSeconds, zone)
                isFirstRecordBeforeDateMarker = firstRecordUtcEpochDay != secondRecordUtcEpochDay
            }

            result.add(PageItem.Record(firstRecord, isFirstRecordBeforeDateMarker))

            var currentRecordLocalEpochDay = secondRecordLocalEpochDay

            for (i in 1 until dataSize) {
                val record = recordData[i]
                val epochDay = currentRecordLocalEpochDay

                if (epochDay != currentSectionLocalEpochDay) {
                    currentSectionLocalEpochDay = epochDay

                    result.add(PageItem.DateMarker(epochDay))
                }

                var isBeforeDateMarker = false

                if (i < dataSize - 1) {
                    currentRecordLocalEpochDay = TimeUtils.toZonedEpochDays(recordData[i + 1].epochSeconds, zone)
                    isBeforeDateMarker = currentRecordLocalEpochDay != currentSectionLocalEpochDay
                }

                result.add(PageItem.Record(record, isBeforeDateMarker))
            }

            result
        } else {
            emptyList()
        }
    }

    companion object {
        private const val TAG = "HomeDataSource"

        private val TABLES = arrayOf("records", "record_badges", "record_to_badge_relations")
    }
}