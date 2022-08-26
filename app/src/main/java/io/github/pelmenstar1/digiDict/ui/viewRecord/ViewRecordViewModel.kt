package io.github.pelmenstar1.digiDict.ui.viewRecord

import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.pelmenstar1.digiDict.common.DataLoadStateManager
import io.github.pelmenstar1.digiDict.common.ui.SingleDataLoadStateViewModel
import io.github.pelmenstar1.digiDict.common.viewModelAction
import io.github.pelmenstar1.digiDict.data.RecordDao
import io.github.pelmenstar1.digiDict.data.RecordWithBadges
import io.github.pelmenstar1.digiDict.data.SearchPreparedRecordDao
import io.github.pelmenstar1.digiDict.widgets.AppWidgetUpdater
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapMerge
import javax.inject.Inject

@HiltViewModel
class ViewRecordViewModel @Inject constructor(
    private val recordDao: RecordDao,
    private val preparedRecordDao: SearchPreparedRecordDao,
    private val listAppWidgetUpdater: AppWidgetUpdater
) : SingleDataLoadStateViewModel<RecordWithBadges?>(TAG) {
    override val canRefreshAfterSuccess: Boolean
        get() = false

    private val idFlow = MutableStateFlow<Int?>(null)

    var id: Int = -1
        set(value) {
            field = value

            idFlow.value = value
        }

    val deleteAction = viewModelAction(TAG) {
        id.let {
            recordDao.deleteById(it)
            preparedRecordDao.deleteById(it)
        }

        listAppWidgetUpdater.updateAllWidgets()
    }

    override fun DataLoadStateManager.FlowBuilder<RecordWithBadges?>.buildDataFlow() = fromFlow {
        idFlow.filterNotNull().flatMapMerge { id ->
            recordDao.getRecordWithBadgesFlowById(id)
        }
    }

    fun delete() = deleteAction.run()

    companion object {
        private const val TAG = "ViewRecordVM"
    }
}