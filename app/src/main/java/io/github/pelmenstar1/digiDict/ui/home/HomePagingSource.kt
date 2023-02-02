package io.github.pelmenstar1.digiDict.ui.home

import android.util.Log
import androidx.paging.PagingSource
import androidx.paging.PagingState
import androidx.room.InvalidationTracker
import androidx.room.withTransaction
import io.github.pelmenstar1.digiDict.common.time.TimeUtils
import io.github.pelmenstar1.digiDict.data.AppDatabase
import io.github.pelmenstar1.digiDict.data.ConciseRecordWithBadges
import io.github.pelmenstar1.digiDict.data.HomeSortType
import io.github.pelmenstar1.digiDict.data.getConciseRecordsWithBadgesForHome
import java.util.*

class HomePagingSource(
    private val appDatabase: AppDatabase,
    private val sortType: HomeSortType
) : PagingSource<Int, HomePageItem>() {
    private val observer = object : InvalidationTracker.Observer(TABLES) {
        override fun onInvalidated(tables: MutableSet<String>) {
            invalidate()
        }
    }

    private var itemCount = -1
    private val recordDao = appDatabase.recordDao()

    init {
        val invalidationTracker = appDatabase.invalidationTracker
        invalidationTracker.addObserver(observer)

        registerInvalidatedCallback {
            invalidationTracker.removeObserver(observer)
        }
    }

    private fun queryItemCount(): Int {
        val countStatement = appDatabase.compileStatement("SELECT COUNT(*) FROM records")

        return countStatement.use {
            it.simpleQueryForLong().toInt()
        }
    }

    override fun getRefreshKey(state: PagingState<Int, HomePageItem>): Int? {
        return when (val anchorPosition = state.anchorPosition) {
            null -> null
            else -> maxOf(0, anchorPosition - (state.config.initialLoadSize / 2))
        }
    }

    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, HomePageItem> {
        return try {
            appDatabase.withTransaction {
                if (itemCount >= 0) {
                    load(params, itemCount)
                } else {
                    itemCount = queryItemCount()

                    load(params, itemCount)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "during loading", e)

            LoadResult.Error(e)
        }
    }

    private suspend fun load(params: LoadParams<Int>, count: Int): LoadResult.Page<Int, HomePageItem> {
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

        val recordData = appDatabase.getConciseRecordsWithBadgesForHome(limit, offset, sortType)
        val recordDataSize = recordData.size

        // For now, the only purpose of computePageResult is to add date markers where neccessary.
        // Doing it when sorting is not related to dates has no sense and very strange.
        //
        // As computePageResult has a single purpose, no flags are added to control the transformation for now.
        val result = if (sortType == HomeSortType.NEWEST || sortType == HomeSortType.OLDEST) {
            computePageResult(recordData, sortType)
        } else {
            recordData.map { HomePageItem.Record(it, isBeforeDateMarker = false) }
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
        sortType: HomeSortType
    ): List<HomePageItem> {
        val dataSize = recordData.size

        return if (dataSize > 0) {
            val zone = TimeZone.getDefault()

            val result = ArrayList<HomePageItem>((dataSize * 3) / 2)

            val firstRecord = recordData[0]
            var currentSectionEpochDay = TimeUtils.toZonedEpochDays(firstRecord.epochSeconds, zone)
            val firstRecordIdWithEpochDay = if (sortType == HomeSortType.NEWEST) {
                recordDao.getFirstRecordIdWithEpochDayOrderByEpochDayDesc(currentSectionEpochDay)
            } else {
                recordDao.getFirstRecordIdWithEpochDayOrderByEpochDayAsc(currentSectionEpochDay)
            }

            if (firstRecordIdWithEpochDay == firstRecord.id) {
                result.add(HomePageItem.DateMarker(currentSectionEpochDay))
            }

            var isFirstRecordBeforeDateMarker = false
            var secondRecordEpochDay = -1L

            if (dataSize > 1) {
                secondRecordEpochDay = TimeUtils.toZonedEpochDays(recordData[1].epochSeconds, zone)
                isFirstRecordBeforeDateMarker = secondRecordEpochDay != currentSectionEpochDay
            }

            result.add(HomePageItem.Record(firstRecord, isFirstRecordBeforeDateMarker))

            var currentRecordEpochDay = secondRecordEpochDay

            for (i in 1 until dataSize) {
                val record = recordData[i]
                val epochDay = currentRecordEpochDay

                if (epochDay != currentSectionEpochDay) {
                    currentSectionEpochDay = epochDay

                    result.add(HomePageItem.DateMarker(epochDay))
                }

                var isBeforeDateMarker = false

                if (i < dataSize - 1) {
                    currentRecordEpochDay = TimeUtils.toZonedEpochDays(recordData[i + 1].epochSeconds, zone)
                    isBeforeDateMarker = currentRecordEpochDay != currentSectionEpochDay
                }

                result.add(HomePageItem.Record(record, isBeforeDateMarker))
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