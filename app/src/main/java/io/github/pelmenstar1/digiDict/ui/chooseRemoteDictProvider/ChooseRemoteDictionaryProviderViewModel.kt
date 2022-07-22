package io.github.pelmenstar1.digiDict.ui.chooseRemoteDictProvider

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.pelmenstar1.digiDict.data.AppDatabase
import io.github.pelmenstar1.digiDict.data.RemoteDictionaryProviderInfo
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ChooseRemoteDictionaryProviderViewModel @Inject constructor(
    appDatabase: AppDatabase
) : ViewModel() {
    private val remoteDictProviderDao = appDatabase.remoteDictionaryProviderDao()
    private val remoteDictProviderStatsDao = appDatabase.remoteDictionaryProviderStatsDao()

    val providers = remoteDictProviderDao.getAllFlow()

    suspend fun getMostUsedProvider(): RemoteDictionaryProviderInfo? {
        return remoteDictProviderStatsDao.getMostUsedProviderStats()?.let {
            remoteDictProviderDao.getById(it.id)
        }
    }

    fun onRemoteDictionaryProviderUsed(provider: RemoteDictionaryProviderInfo) {
        viewModelScope.launch {
            remoteDictProviderStatsDao.incrementVisitCount(provider.id)
        }
    }
}