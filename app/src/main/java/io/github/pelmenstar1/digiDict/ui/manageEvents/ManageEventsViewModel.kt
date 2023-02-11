package io.github.pelmenstar1.digiDict.ui.manageEvents

import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.pelmenstar1.digiDict.common.DataLoadStateManager
import io.github.pelmenstar1.digiDict.common.android.viewModelAction
import io.github.pelmenstar1.digiDict.common.time.CurrentEpochSecondsProvider
import io.github.pelmenstar1.digiDict.common.time.get
import io.github.pelmenstar1.digiDict.common.ui.SingleDataLoadStateViewModel
import io.github.pelmenstar1.digiDict.data.EventDao
import io.github.pelmenstar1.digiDict.data.EventInfo
import javax.inject.Inject

@HiltViewModel
class ManageEventsViewModel @Inject constructor(
    private val eventDao: EventDao,
    private val currentEpochSecondsProvider: CurrentEpochSecondsProvider
) : SingleDataLoadStateViewModel<Array<EventInfo>>(TAG) {
    override val canRefreshAfterSuccess: Boolean
        get() = true

    val isStartEventEnabledFlow = eventDao.isAllEventsEndedFlow()

    val deleteAction = viewModelAction(TAG) { id: Int ->
        eventDao.delete(id)
    }

    val stopEventAction = viewModelAction(TAG) { id: Int ->
        val nowEpochSeconds = currentEpochSecondsProvider.get { Utc }

        eventDao.updateEndEpochSecondsById(id, nowEpochSeconds)
    }

    override fun DataLoadStateManager.FlowBuilder<Array<EventInfo>>.buildDataFlow() = fromFlow {
        eventDao.getAllFlow()
    }

    fun delete(id: Int) {
        deleteAction.run(id)
    }

    fun stopEvent(id: Int) {
        stopEventAction.run(id)
    }

    companion object {
        private const val TAG = "ManageEventsVM"
    }
}