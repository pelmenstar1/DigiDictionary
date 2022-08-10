package io.github.pelmenstar1.digiDict.ui.settings

import android.annotation.SuppressLint
import android.content.Context
import android.util.Log
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavController
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.pelmenstar1.digiDict.backup.RecordImportExportManager
import io.github.pelmenstar1.digiDict.common.DataLoadStateManager
import io.github.pelmenstar1.digiDict.common.serialization.ValidationException
import io.github.pelmenstar1.digiDict.common.ui.SingleDataLoadStateViewModel
import io.github.pelmenstar1.digiDict.data.RecordDao
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
    private val recordDao: RecordDao
) : SingleDataLoadStateViewModel<AppPreferences.Snapshot>(TAG) {
    private val _messageFlow = MutableStateFlow<SettingsMessage?>(null)
    val messageFlow = _messageFlow.asStateFlow()

    override fun DataLoadStateManager.FlowBuilder<AppPreferences.Snapshot>.buildDataFlow() = fromFlow {
        appPreferences.getSnapshotFlow()
    }

    fun <T : Any> changePreferenceValue(entry: AppPreferences.Entry<T>, value: T) {
        viewModelScope.launch(Dispatchers.IO) {
            appPreferences.set(entry, value)
        }
    }

    fun exportData(context: Context) {
        viewModelScope.launch {
            try {
                val showMessage = RecordImportExportManager.export(context, recordDao)

                if (showMessage) {
                    _messageFlow.value = SettingsMessage.EXPORT_SUCCESS
                }
            } catch (e: ValidationException) {
                _messageFlow.value = SettingsMessage.INVALID_FILE
            } catch (e: Exception) {
                Log.e(TAG, "during export", e)

                _messageFlow.value = SettingsMessage.EXPORT_ERROR
            }
        }
    }

    fun importData(context: Context, navController: NavController) {
        viewModelScope.launch {
            try {
                val status = RecordImportExportManager.import(
                    context,
                    recordDao
                )

                when (status) {
                    RecordImportExportManager.IMPORT_SUCCESS_NO_RESOLVE -> {
                        navController.navigate(SettingsFragmentDirections.actionSettingsToResolveImportConflicts())
                    }
                    RecordImportExportManager.IMPORT_SUCCESS_RESOLVE -> {
                        _messageFlow.value = SettingsMessage.IMPORT_SUCCESS
                    }
                }
            } catch (e: ValidationException) {
                _messageFlow.value = SettingsMessage.INVALID_FILE
            } catch (e: Exception) {
                Log.e(TAG, "during export", e)

                _messageFlow.value = SettingsMessage.IMPORT_ERROR
            }
        }
    }

    companion object {
        private const val TAG = "SettingsVM"
    }
}