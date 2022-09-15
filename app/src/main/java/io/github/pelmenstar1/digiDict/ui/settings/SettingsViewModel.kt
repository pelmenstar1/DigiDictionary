package io.github.pelmenstar1.digiDict.ui.settings

import android.annotation.SuppressLint
import android.util.Log
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.pelmenstar1.digiDict.common.DataLoadStateManager
import io.github.pelmenstar1.digiDict.common.ProgressReporter
import io.github.pelmenstar1.digiDict.common.trackProgressWith
import io.github.pelmenstar1.digiDict.common.ui.SingleDataLoadStateViewModel
import io.github.pelmenstar1.digiDict.data.AppDatabase
import io.github.pelmenstar1.digiDict.prefs.AppPreferences
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
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

    private val _messageFlow = MutableStateFlow<SettingsMessage?>(null)
    val messageFlow = _messageFlow.asStateFlow()

    private val progressReporter = ProgressReporter()
    val operationProgressFlow = progressReporter.progressFlow

    val operationErrorFlow = MutableSharedFlow<Throwable?>(replay = 1)

    override fun DataLoadStateManager.FlowBuilder<AppPreferences.Snapshot>.buildDataFlow() = fromFlow {
        appPreferences.getSnapshotFlow()
    }

    fun <T : Any> changePreferenceValue(entry: AppPreferences.Entry<T>, value: T) {
        viewModelScope.launch(Dispatchers.IO) {
            appPreferences.set(entry, value)
        }
    }

    fun deleteAllRecords() {
        progressReporter.reset()

        viewModelScope.launch {
            try {
                trackProgressWith(progressReporter) {
                    recordDao.deleteAll()
                }

                _messageFlow.value = SettingsMessage.DELETE_ALL_SUCCESS
            } catch (e: Exception) {
                if (e !is CancellationException) {
                    Log.e(TAG, "during deleteAllRecords", e)

                    operationErrorFlow.emit(e)
                    _messageFlow.value = SettingsMessage.DB_ERROR
                }
            }
        }
    }

    companion object {
        private const val TAG = "SettingsVM"
    }
}