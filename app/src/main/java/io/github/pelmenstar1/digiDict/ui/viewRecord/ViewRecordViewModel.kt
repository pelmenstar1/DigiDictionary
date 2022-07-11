package io.github.pelmenstar1.digiDict.ui.viewRecord

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavController
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.pelmenstar1.digiDict.data.AppDatabase
import io.github.pelmenstar1.digiDict.data.Record
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

    private val _messageFlow = MutableStateFlow<ViewRecordMessage?>(null)
    val messageFlow = _messageFlow.asStateFlow()

    private val _recordFlow = MutableStateFlow<Result<Record?>?>(null)
    val recordFlow = _recordFlow.asStateFlow()

    var onDeleteConfirmation: (suspend () -> Boolean)? = null

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
            _recordFlow.value = try {
                val value = recordDao.getRecordById(id)

                Result.success(value)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    fun delete(navController: NavController) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val isConfirmed = onDeleteConfirmation?.invoke() ?: true

                if (isConfirmed) {
                    recordDao.deleteById(id)

                    withContext(Dispatchers.Main) {
                        listAppWidgetUpdater.updateAllWidgets()

                        navController.popBackStack()
                    }
                }

                _messageFlow.value = null
            } catch (e: Exception) {
                Log.e(TAG, "during delete", e)

                _messageFlow.value = ViewRecordMessage.DB_ERROR
            }
        }
    }

    fun edit(navController: NavController) {
        val directions = ViewRecordFragmentDirections.actionViewRecordToAddEditRecord(id)

        navController.navigate(directions)
    }

    companion object {
        private const val TAG = "ViewRecordVM"
    }
}