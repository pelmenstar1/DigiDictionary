package io.github.pelmenstar1.digiDict.ui.manageRemoteDictProviders

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.pelmenstar1.digiDict.data.RemoteDictionaryProviderDao
import io.github.pelmenstar1.digiDict.data.RemoteDictionaryProviderInfo
import io.github.pelmenstar1.digiDict.utils.Event
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ManageRemoteDictionaryProvidersViewModel @Inject constructor(
    private val remoteDictProviderDao: RemoteDictionaryProviderDao
) : ViewModel() {
    val providersFlow: Flow<Array<RemoteDictionaryProviderInfo>?>
    private val retryFlow = MutableStateFlow<Array<RemoteDictionaryProviderInfo>?>(null)

    val onLoadingError = Event()
    val onDeleteError = Event()

    init {
        val daoFlow = remoteDictProviderDao.getAllFlow()

        providersFlow = merge(daoFlow, retryFlow)
    }

    fun loadProviders() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                retryFlow.value = remoteDictProviderDao.getAll()
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