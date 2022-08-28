package io.github.pelmenstar1.digiDict.ui.manageRemoteDictProviders

import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.pelmenstar1.digiDict.common.DataLoadStateManager
import io.github.pelmenstar1.digiDict.common.ui.SingleDataLoadStateViewModel
import io.github.pelmenstar1.digiDict.common.viewModelAction
import io.github.pelmenstar1.digiDict.data.RemoteDictionaryProviderDao
import io.github.pelmenstar1.digiDict.data.RemoteDictionaryProviderInfo
import io.github.pelmenstar1.digiDict.data.RemoteDictionaryProviderStatsDao
import javax.inject.Inject

@HiltViewModel
class ManageRemoteDictionaryProvidersViewModel @Inject constructor(
    private val remoteDictProviderDao: RemoteDictionaryProviderDao,
    private val remoteDictProviderStatsDao: RemoteDictionaryProviderStatsDao
) : SingleDataLoadStateViewModel<Array<RemoteDictionaryProviderInfo>>(TAG) {
    override val canRefreshAfterSuccess: Boolean
        get() = true

    val deleteAction = viewModelAction<RemoteDictionaryProviderInfo>(TAG) { provider ->
        remoteDictProviderDao.delete(provider)
        remoteDictProviderStatsDao.deleteById(provider.id)
    }

    override fun DataLoadStateManager.FlowBuilder<Array<RemoteDictionaryProviderInfo>>.buildDataFlow() = fromFlow {
        remoteDictProviderDao.getAllFlow()
    }

    fun delete(provider: RemoteDictionaryProviderInfo) = deleteAction.run(provider)

    companion object {
        private const val TAG = "ManageRDP_VM"
    }
}