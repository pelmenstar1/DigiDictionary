package io.github.pelmenstar1.digiDict.ui.eventInfo

import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.pelmenstar1.digiDict.common.DataLoadState
import io.github.pelmenstar1.digiDict.common.DataLoadStateManager
import io.github.pelmenstar1.digiDict.common.ui.SingleDataLoadStateViewModel
import io.github.pelmenstar1.digiDict.data.EventDao
import io.github.pelmenstar1.digiDict.data.RecordDao
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.map
import javax.inject.Inject

@HiltViewModel
class EventInfoViewModel @Inject constructor(
    private val eventDao: EventDao,
    private val recordDao: RecordDao
) : SingleDataLoadStateViewModel<EventInfoWithStats>(TAG) {
    private val eventIdFlow = MutableStateFlow<Int?>(null)

    var eventId: Int = -1
        set(value) {
            field = value

            eventIdFlow.value = value
        }

    override val canRefreshAfterSuccess: Boolean
        get() = false

    override fun DataLoadStateManager.FlowBuilder<EventInfoWithStats>.buildDataFlow(): Flow<DataLoadState<EventInfoWithStats>> {
        return fromFlow {
            eventIdFlow.filterNotNull().map { id ->
                val basicInfo = eventDao.getById(id)!!
                val startTime = basicInfo.startEpochSeconds
                val endTime = basicInfo.endEpochSeconds

                val interpolatedEndTime = if (endTime < 0) Long.MAX_VALUE else endTime
                val totalRecordsAdded = recordDao.countRecordsDuringTimeRange(startTime, interpolatedEndTime)

                EventInfoWithStats(
                    basicInfo.id,
                    basicInfo.name,
                    startTime, endTime,
                    totalRecordsAdded
                )
            }
        }
    }

    companion object {
        private const val TAG = "EventInfoViewModel"
    }
}