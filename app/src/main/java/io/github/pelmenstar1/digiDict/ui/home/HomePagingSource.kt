package io.github.pelmenstar1.digiDict.ui.home

import android.util.Log
import androidx.paging.PagingSource
import androidx.paging.PagingState
import androidx.room.InvalidationTracker
import androidx.room.withTransaction
import io.github.pelmenstar1.digiDict.data.AppDatabase
import io.github.pelmenstar1.digiDict.data.ConciseRecordWithBadges
import io.github.pelmenstar1.digiDict.data.RecordTable

class HomePagingSource(private val appDatabase: AppDatabase) : PagingSource<Int, ConciseRecordWithBadges>() {
    private val observer = object : InvalidationTracker.Observer(TABLE_NAME_ARRAY) {
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
        val countStatement = appDatabase.compileStatement("SELECT COUNT(*) FROM $TABLE_NAME")

        return countStatement.use {
            it.simpleQueryForLong().toInt()
        }
    }

    override fun getRefreshKey(state: PagingState<Int, ConciseRecordWithBadges>): Int? {
        return when (val anchorPosition = state.anchorPosition) {
            null -> null
            else -> maxOf(0, anchorPosition - (state.config.initialLoadSize / 2))
        }
    }

    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, ConciseRecordWithBadges> {
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

    private suspend fun load(params: LoadParams<Int>, count: Int): LoadResult.Page<Int, ConciseRecordWithBadges> {
        val key = params.key ?: 0
        val limit = when (params) {
            is LoadParams.Prepend ->
                if (key < params.loadSize) {
                    key
                } else {
                    params.loadSize
                }
            else -> params.loadSize
        }

        val offset = when (params) {
            is LoadParams.Prepend ->
                if (key < params.loadSize) {
                    0
                } else {
                    key - params.loadSize
                }
            is LoadParams.Append -> key
            is LoadParams.Refresh ->
                if (key >= count) {
                    maxOf(0, count - params.loadSize)
                } else {
                    key
                }
        }

        val data = recordDao.getConciseRecordsWithBadgesLimitOffset(limit, offset)
        val dataSize = data.size

        val nextPosToLoad = offset + dataSize

        return LoadResult.Page(
            data,
            prevKey = if (offset <= 0 || dataSize == 0) null else offset,
            nextKey = if (dataSize == 0 || dataSize < limit || nextPosToLoad >= count) {
                null
            } else {
                nextPosToLoad
            }
        )
    }

    companion object {
        private const val TABLE_NAME = RecordTable.name
        private const val TAG = "HomeDataSource"

        private val TABLE_NAME_ARRAY = arrayOf(TABLE_NAME)
    }
}