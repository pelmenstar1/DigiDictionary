package io.github.pelmenstar1.digiDict.ui.viewRecord

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.pelmenstar1.digiDict.data.AppDatabase
import io.github.pelmenstar1.digiDict.data.Record
import io.github.pelmenstar1.digiDict.utils.Event
import io.github.pelmenstar1.digiDict.widgets.AppWidgetUpdater
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@HiltViewModel
class ViewRecordViewModel @Inject constructor(
    appDatabase: AppDatabase,
    private val listAppWidgetUpdater: AppWidgetUpdater
) : ViewModel() {
    private val recordDao = appDatabase.recordDao()

    private val _recordFlow = MutableStateFlow<Record?>(null)
    val recordFlow = _recordFlow.asStateFlow()

    val onDeleteError = Event()
    val onLoadingError = Event()
    val onRecordDeleted = Event()

    var id: Int = -1
        set(value) {
            field = value

            refreshRecord()
        }

    init {
        appDatabase.addRecordTableObserver(this) {
            refreshRecord()
        }
    }

    fun refreshRecord() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                _recordFlow.value = recordDao.getRecordById(id)
            } catch (e: Exception) {
                Log.e(TAG, "during loading the record", e)

                onLoadingError.raiseOnMainThread()
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