package io.github.pelmenstar1.digiDict.ui.manageRemoteDictProviders

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.pelmenstar1.digiDict.data.RemoteDictionaryProviderDao
import io.github.pelmenstar1.digiDict.data.RemoteDictionaryProviderInfo
import io.github.pelmenstar1.digiDict.data.RemoteDictionaryProviderStatsDao
import io.github.pelmenstar1.digiDict.utils.DataLoadStateManager
import io.github.pelmenstar1.digiDict.utils.Event
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ManageRemoteDictionaryProvidersViewModel @Inject constructor(
    private val remoteDictProviderDao: RemoteDictionaryProviderDao,
    private val remoteDictProviderStatsDao: RemoteDictionaryProviderStatsDao
) : ViewModel() {
    private val providersStateManager = DataLoadStateManager<Array<RemoteDictionaryProviderInfo>>(TAG)

    val providersStateFlow = providersStateManager.buildFlow(viewModelScope) {
        fromFlow {
            remoteDictProviderDao.getAllFlow()
        }
    }

    val onDeleteError = Event()

    fun retryLoadProviders() {
        providersStateManager.retry()
    }

    fun delete(provider: RemoteDictionaryProviderInfo) {
        viewModelScope.launch(Dispatchers.Default) {
            try {
                remoteDictProviderDao.delete(provider)
                remoteDictProviderStatsDao.deleteById(provider.id)
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