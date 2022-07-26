package io.github.pelmenstar1.digiDict.ui.manageRemoteDictProviders

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.pelmenstar1.digiDict.data.AppDatabase
import io.github.pelmenstar1.digiDict.data.RemoteDictionaryProviderInfo
import io.github.pelmenstar1.digiDict.utils.Event
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ManageRemoteDictionaryProvidersViewModel @Inject constructor(
    appDatabase: AppDatabase
) : ViewModel() {
    private val remoteDictProviderDao = appDatabase.remoteDictionaryProviderDao()

    private val _providersFlow = MutableStateFlow<Array<RemoteDictionaryProviderInfo>?>(null)
    val providersFlow = _providersFlow.asStateFlow()

    val onLoadingError = Event()
    val onDeleteError = Event()

    init {
        loadProviders()
    }

    fun loadProviders() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                _providersFlow.value = remoteDictProviderDao.getAll()
            } catch (e: Exception) {
                Log.e(TAG, "during getAll()", e)

                onLoadingError.raiseOnMainThread()
            }
        }
    }

    fun delete(provider: RemoteDictionaryProviderInfo) {
        viewModelScope.launch(Dispatchers.Default) {
            try {
                remoteDictProviderDao.delete(provider)
            } catch (e: Exception) {
                Log.e(TAG, "during delete", e)

                onDeleteError.raiseOnMainThread()
            }
        }
    }

    companion object {
        private const val TAG = "ManageRDP_VM"
    }
}