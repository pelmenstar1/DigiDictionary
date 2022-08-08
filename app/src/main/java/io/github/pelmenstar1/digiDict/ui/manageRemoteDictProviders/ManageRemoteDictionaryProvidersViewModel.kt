package io.github.pelmenstar1.digiDict.ui.manageRemoteDictProviders

import android.util.Log
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.pelmenstar1.digiDict.common.DataLoadStateManager
import io.github.pelmenstar1.digiDict.common.Event
import io.github.pelmenstar1.digiDict.common.ui.SingleDataLoadStateViewModel
import io.github.pelmenstar1.digiDict.data.RemoteDictionaryProviderDao
import io.github.pelmenstar1.digiDict.data.RemoteDictionaryProviderInfo
import io.github.pelmenstar1.digiDict.data.RemoteDictionaryProviderStatsDao
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ManageRemoteDictionaryProvidersViewModel @Inject constructor(
    private val remoteDictProviderDao: RemoteDictionaryProviderDao,
    private val remoteDictProviderStatsDao: RemoteDictionaryProviderStatsDao
) : SingleDataLoadStateViewModel<Array<RemoteDictionaryProviderInfo>>(TAG) {
    val onDeleteError = Event()

    override fun DataLoadStateManager.FlowBuilder<Array<RemoteDictionaryProviderInfo>>.buildDataFlow() = fromFlow {
        remoteDictProviderDao.getAllFlow()
    }

    fun delete(provider: RemoteDictionaryProviderInfo) {
        viewModelScope.launch(Dispatchers.Default) {
            try {
                remoteDictProviderDao.delete(provider)
                remoteDictProviderStatsDao.deleteById(provider.id)
            } catch (e: Exception) {
                Log.e(TAG, "during delete", e)

                onDeleteError.raiseOnMainThreadIfNotCancellation(e)
            }
        }
    }

    companion object {
        private const val TAG = "ManageRDP_VM"
    }
}