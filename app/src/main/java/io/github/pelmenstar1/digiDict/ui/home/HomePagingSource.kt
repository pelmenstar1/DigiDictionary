package io.github.pelmenstar1.digiDict.ui.home

import android.util.Log
import androidx.paging.PagingSource
import androidx.paging.PagingState
import androidx.room.InvalidationTracker
import androidx.room.withTransaction
import io.github.pelmenstar1.digiDict.common.time.SECONDS_IN_DAY
import io.github.pelmenstar1.digiDict.data.AppDatabase
import io.github.pelmenstar1.digiDict.data.ConciseRecordWithBadges
import io.github.pelmenstar1.digiDict.data.HomeSortType
import io.github.pelmenstar1.digiDict.data.getConciseRecordsWithBadgesForHome

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
            if (itemCount >= 0) {
                load(params, itemCount)
            } else {
                appDatabase.withTransaction {
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

        val result = computePageResult(recordData)

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

    private suspend fun computePageResult(recordData: Array<out ConciseRecordWithBadges>): List<HomePageItem> {
        val dataSize = recordData.size

        if (dataSize > 0) {
            val result = ArrayList<HomePageItem>(dataSize + (dataSize * 3) / 2)

            val firstRecord = recordData[0]
            var currentEpochDay = firstRecord.epochSeconds / SECONDS_IN_DAY
            val firstRecordIdWithEpochDay = recordDao.getFirstRecordIdWithEpochDay(currentEpochDay)

            if (firstRecordIdWithEpochDay == firstRecord.id) {
                result.add(HomePageItem.DateMarker(currentEpochDay))
            }

            var isFirstRecordBeforeDateMarker = false
            if (recordData.size > 1) {
                val nextRecordEpochDay = recordData[1].epochSeconds / SECONDS_IN_DAY
                isFirstRecordBeforeDateMarker = nextRecordEpochDay != currentEpochDay
            }

            result.add(HomePageItem.Record(firstRecord, isFirstRecordBeforeDateMarker))

            for (i in 1 until dataSize) {
                val record = recordData[i]
                val epochDay = record.epochSeconds / SECONDS_IN_DAY

                if (epochDay != currentEpochDay) {
                    currentEpochDay = epochDay

                    result.add(HomePageItem.DateMarker(epochDay))
                }

                var isBeforeDateMarker = false

                if (i < dataSize - 1) {
                    val nextEpochDay = recordData[i + 1].epochSeconds / SECONDS_IN_DAY

                    isBeforeDateMarker = nextEpochDay != currentEpochDay
                }

                result.add(HomePageItem.Record(record, isBeforeDateMarker))
            }

            return result
        } else {
            return emptyList()
        }
    }

    companion object {
        private const val TAG = "HomeDataSource"

        private val TABLES = arrayOf("records", "record_badges", "record_to_badge_relations")
    }
}