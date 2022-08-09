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
import kotlinx.coroutines.withContext
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

    fun <T : Any> changePreferenceValue(entry: AppPreferences.Entry<T>, value: T, onPrefChanged: (() -> Unit)? = null) {
        viewModelScope.launch(Dispatchers.IO) {
            appPreferences.set(entry, value)

            onPrefChanged?.also {
                withContext(Dispatchers.Main) {
                    it.invoke()
                }
            }
        }
    }

    private inline fun importExportInternal(
        successMessage: SettingsMessage,
        errorMessage: SettingsMessage,
        actionName: String,
        crossinline block: suspend () -> Boolean
    ) {
        viewModelScope.launch {
            try {
                val showMessage = block()

                if (showMessage) {
                    _messageFlow.value = successMessage
                }
            } catch (e: NullPointerException) {
                // it means user has selected no file, so just eat an exception.
            } catch (e: ValidationException) {
                _messageFlow.value = SettingsMessage.INVALID_FILE
            } catch (e: Exception) {
                Log.e(TAG, "during $actionName", e)

                _messageFlow.value = errorMessage
            }
        }
    }

    fun exportData(context: Context) {
        importExportInternal(
            successMessage = SettingsMessage.EXPORT_SUCCESS,
            errorMessage = SettingsMessage.EXPORT_ERROR,
            actionName = "export"
        ) {
            RecordImportExportManager.export(context, recordDao)

            true
        }
    }

    fun importData(context: Context, navController: NavController) {
        importExportInternal(
            successMessage = SettingsMessage.IMPORT_SUCCESS,
            errorMessage = SettingsMessage.IMPORT_ERROR,
            actionName = "import"
        ) {
            val shouldResolveConflicts = RecordImportExportManager.import(
                context,
                recordDao
            )

            if (shouldResolveConflicts) {
                navController.navigate(SettingsFragmentDirections.actionSettingsToResolveImportConflicts())
                false
            } else {
                true
            }
        }
    }

    companion object {
        private const val TAG = "SettingsVM"
    }
}