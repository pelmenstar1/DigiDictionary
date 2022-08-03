package io.github.pelmenstar1.digiDict.ui.viewRecord

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.pelmenstar1.digiDict.data.Record
import io.github.pelmenstar1.digiDict.data.RecordDao
import io.github.pelmenstar1.digiDict.utils.DataLoadStateManager
import io.github.pelmenstar1.digiDict.utils.Event
import io.github.pelmenstar1.digiDict.widgets.AppWidgetUpdater
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapMerge
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@HiltViewModel
class ViewRecordViewModel @Inject constructor(
    private val recordDao: RecordDao,
    private val listAppWidgetUpdater: AppWidgetUpdater
) : ViewModel() {
    private val stateManager = DataLoadStateManager<Record?>(TAG)

    private val idFlow = MutableStateFlow<Int?>(null)

    var id: Int = -1
        set(value) {
            field = value

            idFlow.value = value
        }

    val recordStateFlow = stateManager.buildFlow(viewModelScope) {
        fromFlow {
            idFlow.filterNotNull().flatMapMerge { id ->
                recordDao.getRecordFlowById(id)
            }
        }
    }

    val onDeleteError = Event()
    val onRecordDeleted = Event()

    fun refreshRecord() {
        stateManager.retry()
    }

    fun delete() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                recordDao.deleteById(id)

                withContext(Dispatchers.Main) {
                    listAppWidgetUpdater.updateAllWidgets()

                    onRecordDeleted.raise()
                }
            } catch (e: Exception) {
                Log.e(TAG, "during delete", e)

                onDeleteError.raiseOnMainThread()
            }
        }
    }

    companion object {
        private const val TAG = "ViewRecordVM"
    }
}