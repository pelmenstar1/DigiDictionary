package io.github.pelmenstar1.digiDict.ui.eventInfoRecords

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.cachedIn
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.pelmenstar1.digiDict.common.time.EpochSecondsRange
import io.github.pelmenstar1.digiDict.data.AppDatabase
import io.github.pelmenstar1.digiDict.data.RecordSortType
import io.github.pelmenstar1.digiDict.ui.paging.AppPagingSource
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * A view model for [EventInfoRecordsFragment]
 */
@HiltViewModel
class EventInfoRecordsViewModel @Inject constructor(
    private val appDatabase: AppDatabase
) : ViewModel() {
    private class TimeRangeState(@JvmField val startTime: Long, @JvmField val endTime: Long) {
        val isLoaded: Boolean
            get() = (startTime or endTime) >= 0

        fun toEpochSecondsRange() = EpochSecondsRange(startTime, endTime)

        companion object {
            @JvmField
            val ERROR = TimeRangeState(-1L, -1L)
        }
    }

    private val eventDao = appDatabase.eventDao()
    private val eventTimeRangeStateFlow = MutableSharedFlow<TimeRangeState>(replay = 1)
    private val _sortTypeFlow = MutableStateFlow(RecordSortType.NEWEST)

    /**
     * Gets the flow that indicates whether the event is loaded successfully.
     */
    val isEventSuccessfullyLoadedFlow: Flow<Boolean> = eventTimeRangeStateFlow.map { it.isLoaded }

    /**
     * Gets or sets id of the event to show the records that were during that event.
     *
     * The setter is expected to be called once to set the positive id although the default value is `-1`.
     */
    var eventId: Int = -1
        set(value) {
            require(value >= 0) { "eventId is negative" }
            field = value

            scheduleLoadEventTimeRange()
        }

    /**
     * A flow a value to which is emitted any time the [sortType] is changed.
     */
    val sortTypeFlow: Flow<RecordSortType>
        get() = _sortTypeFlow

    /**
     * Sets current [RecordSortType].
     *
     * When value is changed, the paging is not notified about it and it should be explicitly.
     */
    var sortType: RecordSortType
        get() = _sortTypeFlow.value
        set(value) {
            _sortTypeFlow.value = value
        }

    val items = Pager(
        config = PagingConfig(pageSize = 20),
        pagingSourceFactory = {
            AppPagingSource(
                appDatabase,
                sortType,
                getTimeRangeLambda = {
                    eventTimeRangeStateFlow.first { it.isLoaded }.toEpochSecondsRange()
                }
            )
        }
    ).flow.cachedIn(viewModelScope)


    /**
     * Retries to load the event data.
     *
     * It's expected to be called only when [isEventSuccessfullyLoadedFlow] emitted `false`.
     */
    fun retryLoadEvent() {
        scheduleLoadEventTimeRange()
    }

    private fun scheduleLoadEventTimeRange() {
        viewModelScope.launch {
            try {
                val event = eventDao.getById(eventId) ?: throw RuntimeException("The event is expected to be non-null")
                val startTime = event.startEpochSeconds
                val endTime = event.endEpochSeconds
                val positiveEndTime = if (endTime < 0L) Long.MAX_VALUE else endTime

                eventTimeRangeStateFlow.emit(TimeRangeState(startTime, positiveEndTime))
            } catch (e: Exception) {
                if (e !is CancellationException) {
                    Log.e(TAG, null, e)

                    eventTimeRangeStateFlow.emit(TimeRangeState.ERROR)
                }
            }
        }
    }

    companion object {
        private const val TAG = "EventInfoRecordsVM"
    }
}