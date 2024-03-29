package io.github.pelmenstar1.digiDict.ui.settings

import android.annotation.SuppressLint
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.pelmenstar1.digiDict.common.DataLoadStateManager
import io.github.pelmenstar1.digiDict.common.ProgressReporter
import io.github.pelmenstar1.digiDict.common.android.viewModelAction
import io.github.pelmenstar1.digiDict.common.preferences.AppPreferences
import io.github.pelmenstar1.digiDict.common.trackProgressWith
import io.github.pelmenstar1.digiDict.common.ui.SingleDataLoadStateViewModel
import io.github.pelmenstar1.digiDict.data.RecordDao
import io.github.pelmenstar1.digiDict.prefs.DigiDictAppPreferences
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
@SuppressLint("StaticFieldLeak")
class SettingsViewModel @Inject constructor(
    private val appPreferences: DigiDictAppPreferences,
    private val recordDao: RecordDao
) : SingleDataLoadStateViewModel<DigiDictAppPreferences.Snapshot>(TAG) {
    override val canRefreshAfterSuccess: Boolean
        get() = false

    private val progressReporter = ProgressReporter()
    val operationProgressFlow = progressReporter.progressFlow

    val deleteAllRecordsAction = viewModelAction(TAG) {
        trackProgressWith(progressReporter) {
            recordDao.deleteAll()
        }
    }

    override fun DataLoadStateManager.FlowBuilder<DigiDictAppPreferences.Snapshot>.buildDataFlow() = fromFlow {
        appPreferences.getSnapshotFlow()
    }

    fun <T : Any> changePreferenceValue(entry: AppPreferences.Entry<T, DigiDictAppPreferences.Entries>, value: T) {
        viewModelScope.launch(Dispatchers.IO) {
            appPreferences.set(entry, value)
        }
    }

    companion object {
        private const val TAG = "SettingsVM"
    }
}