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
    private val recordDao = appDatabase.recordDao()
    private val searchPreparedRecordDao = appDatabase.searchPreparedRecordDao()

    private val _messageFlow = MutableStateFlow<SettingsMessage?>(null)
    val messageFlow = _messageFlow.asStateFlow()

    private val progressReporter = ProgressReporter()
    val importExportProgressFlow = progressReporter.progressFlow

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
        progressReporter.reset()

        viewModelScope.launch {
            try {
                val showMessage = RecordImportExportManager.export(context, recordDao, progressReporter)

                if (showMessage) {
                    _messageFlow.value = SettingsMessage.EXPORT_SUCCESS
                }
            } catch (e: ValidationException) {
                onOperationError.raiseOnMainThread()

                _messageFlow.value = SettingsMessage.INVALID_FILE
            } catch (e: Exception) {
                Log.e(TAG, "during export", e)

                onOperationError.raiseOnMainThreadIfNotCancellation(e)

                _messageFlow.value = SettingsMessage.EXPORT_ERROR
            }
        }
    }

    fun importData(context: Context) {
        progressReporter.reset()

        viewModelScope.launch {
            try {
                val showMessage = RecordImportExportManager.import(
                    context,
                    appDatabase,
                    progressReporter
                )

                if (showMessage) {
                    _messageFlow.value = SettingsMessage.IMPORT_SUCCESS
                }
            } catch (e: ValidationException) {
                _messageFlow.value = SettingsMessage.INVALID_FILE

                onOperationError.raiseOnMainThread()
            } catch (e: Exception) {
                Log.e(TAG, "during export", e)

                onOperationError.raiseOnMainThreadIfNotCancellation(e)

                _messageFlow.value = SettingsMessage.IMPORT_ERROR
            }
        }
    }

    fun deleteAllRecords() {
        progressReporter.reset()

        viewModelScope.launch {
            try {
                appDatabase.withTransaction {
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