package io.github.pelmenstar1.digiDict.ui.viewRecord

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.pelmenstar1.digiDict.data.Record
import io.github.pelmenstar1.digiDict.data.RecordDao
import io.github.pelmenstar1.digiDict.utils.Event
import io.github.pelmenstar1.digiDict.widgets.AppWidgetUpdater
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@HiltViewModel
class ViewRecordViewModel @Inject constructor(
    private val recordDao: RecordDao,
    private val listAppWidgetUpdater: AppWidgetUpdater
) : ViewModel() {
    private val retryFlow = MutableStateFlow<Record?>(null)

    private var _recordFlow: Flow<Record?>? = null
    val recordFlow: Flow<Record?>?
        get() = _recordFlow

    val onDeleteError = Event()
    val onRefreshError = Event()
    val onRecordDeleted = Event()

    var id: Int = -1
        set(value) {
            field = value

            val daoFlow = recordDao.getRecordFlowById(value)
            _recordFlow = merge(daoFlow, retryFlow)
        }

    fun refreshRecord() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                retryFlow.value = recordDao.getRecordById(id)
            } catch (e: Exception) {
                Log.e(TAG, "during loading the record", e)

                onRefreshError.raiseOnMainThread()
            }
        }
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