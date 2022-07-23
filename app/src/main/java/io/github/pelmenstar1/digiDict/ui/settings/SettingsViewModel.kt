package io.github.pelmenstar1.digiDict.ui.settings

import android.annotation.SuppressLint
import android.content.Context
import android.util.Log
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavController
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import io.github.pelmenstar1.digiDict.backup.RecordImportExportManager
import io.github.pelmenstar1.digiDict.data.AppDatabase
import io.github.pelmenstar1.digiDict.serialization.ValidationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
@SuppressLint("StaticFieldLeak")
class SettingsViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val dataStore: DataStore<Preferences>,
    appDatabase: AppDatabase
) : ViewModel() {
    private val recordDao = appDatabase.recordDao()

    private val _messageFlow = MutableStateFlow<SettingsMessage?>(null)
    val messageFlow = _messageFlow.asStateFlow()

    fun <T> getPreferenceValueFlow(key: Preferences.Key<T>): Flow<T?> {
        return dataStore.data.map { it[key] }
    }

    fun <T> changePreferenceValue(key: Preferences.Key<T>, value: T) {
        viewModelScope.launch(Dispatchers.IO) {
            dataStore.edit {
                it[key] = value
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

    fun exportData() {
        importExportInternal(
            successMessage = SettingsMessage.EXPORT_SUCCESS,
            errorMessage = SettingsMessage.EXPORT_ERROR,
            actionName = "export"
        ) {
            RecordImportExportManager.export(context, recordDao)

            true
        }
    }

    fun importData(navController: NavController) {
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