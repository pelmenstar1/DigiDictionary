package io.github.pelmenstar1.digiDict.ui.settings

import android.annotation.SuppressLint
import android.content.Context
import android.util.Log
import androidx.lifecycle.viewModelScope
import androidx.room.withTransaction
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.pelmenstar1.digiDict.backup.RecordImportExportManager
import io.github.pelmenstar1.digiDict.common.DataLoadStateManager
import io.github.pelmenstar1.digiDict.common.Event
import io.github.pelmenstar1.digiDict.common.ProgressReporter
import io.github.pelmenstar1.digiDict.common.serialization.ValidationException
import io.github.pelmenstar1.digiDict.common.ui.SingleDataLoadStateViewModel
import io.github.pelmenstar1.digiDict.data.AppDatabase
import io.github.pelmenstar1.digiDict.prefs.AppPreferences
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
@SuppressLint("StaticFieldLeak")
class SettingsViewModel @Inject constructor(
    private val appPreferences: AppPreferences,
    private val appDatabase: AppDatabase
) : SingleDataLoadStateViewModel<AppPreferences.Snapshot>(TAG) {
    override val canRefreshAfterSuccess: Boolean
        get() = false

    private val recordDao = appDatabase.recordDao()
    private val searchPreparedRecordDao = appDatabase.searchPreparedRecordDao()

    private val _messageFlow = MutableStateFlow<SettingsMessage?>(null)
    val messageFlow = _messageFlow.asStateFlow()

    private val progressReporter = ProgressReporter()
    val operationProgressFlow = progressReporter.progressFlow

    val onOperationError = Event()

    override fun DataLoadStateManager.FlowBuilder<AppPreferences.Snapshot>.buildDataFlow() = fromFlow {
        appPreferences.getSnapshotFlow()
    }

    fun <T : Any> changePreferenceValue(entry: AppPreferences.Entry<T>, value: T) {
        viewModelScope.launch(Dispatchers.IO) {
            appPreferences.set(entry, value)
        }
    }

    fun exportData(context: Context) {
        importExport(
            operationName = "export",
            operationSuccessMsg = SettingsMessage.EXPORT_SUCCESS,
            operationErrorMsg = SettingsMessage.EXPORT_ERROR,
        ) {
            export(context, recordDao, progressReporter)
        }
    }

    fun importData(context: Context) {
        importExport(
            operationName = "import",
            operationSuccessMsg = SettingsMessage.IMPORT_SUCCESS,
            operationErrorMsg = SettingsMessage.IMPORT_ERROR,
        ) {
            import(context, appDatabase, progressReporter)
        }
    }

    private fun importExport(
        operationName: String,
        operationSuccessMsg: SettingsMessage,
        operationErrorMsg: SettingsMessage,
        operation: suspend RecordImportExportManager.() -> Boolean
    ) {
        progressReporter.reset()

        viewModelScope.launch {
            try {
                val showMessage = RecordImportExportManager.operation()

                if (showMessage) {
                    _messageFlow.value = operationSuccessMsg
                }
            } catch (e: ValidationException) {
                _messageFlow.value = SettingsMessage.INVALID_FILE

                onOperationError.raiseOnMainThread()
            } catch (e: Exception) {
                Log.e(TAG, "during $operationName", e)

                onOperationError.raiseOnMainThreadIfNotCancellation(e)

                _messageFlow.value = operationErrorMsg
            }
        }
    }

    fun deleteAllRecords() {
        progressReporter.reset()

        viewModelScope.launch {
            try {
                appDatabase.withTransaction {
                    progressReporter.onProgress(0, 100)
                    recordDao.deleteAll()
                    progressReporter.onProgress(50, 100)
                    searchPreparedRecordDao.deleteAll()
                    progressReporter.onEnd()
                }

                _messageFlow.value = SettingsMessage.DELETE_ALL_SUCCESS
            } catch (e: Exception) {
                Log.e(TAG, "during deleteAllRecords", e)

                onOperationError.raiseOnMainThreadIfNotCancellation(e)
                _messageFlow.value = SettingsMessage.DB_ERROR
            }
        }
    }

    companion object {
        private const val TAG = "SettingsVM"
    }
}