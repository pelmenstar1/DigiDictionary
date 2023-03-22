package io.github.pelmenstar1.digiDict.ui.wordQueue

import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.pelmenstar1.digiDict.common.DataLoadState
import io.github.pelmenstar1.digiDict.common.DataLoadStateManager
import io.github.pelmenstar1.digiDict.common.android.viewModelAction
import io.github.pelmenstar1.digiDict.common.ui.SingleDataLoadStateViewModel
import io.github.pelmenstar1.digiDict.data.WordQueueDao
import io.github.pelmenstar1.digiDict.data.WordQueueEntry
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

@HiltViewModel
class WordQueueViewModel @Inject constructor(
    private val queueDao: WordQueueDao
) : SingleDataLoadStateViewModel<Array<WordQueueEntry>>(TAG) {
    override val canRefreshAfterSuccess: Boolean
        get() = true

    val removeFromQueueAction = viewModelAction(TAG) { id: Int ->
        queueDao.deleteById(id)
    }

    override fun DataLoadStateManager.FlowBuilder<Array<WordQueueEntry>>.buildDataFlow(): Flow<DataLoadState<Array<WordQueueEntry>>> {
        return fromFlow(queueDao.getAllFlow())
    }

    fun removeFromQueue(id: Int) {
        removeFromQueueAction.run(id)
    }

    companion object {
        private const val TAG = "WordQueueViewModel"
    }
}