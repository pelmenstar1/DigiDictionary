package io.github.pelmenstar1.digiDict.ui.chooseRemoteDictProvider

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.pelmenstar1.digiDict.data.RemoteDictionaryProviderDao
import io.github.pelmenstar1.digiDict.data.RemoteDictionaryProviderInfo
import io.github.pelmenstar1.digiDict.data.RemoteDictionaryProviderStatsDao
import io.github.pelmenstar1.digiDict.prefs.AppPreferences
import io.github.pelmenstar1.digiDict.prefs.get
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ChooseRemoteDictionaryProviderViewModel @Inject constructor(
    private val remoteDictProviderDao: RemoteDictionaryProviderDao,
    private val remoteDictProviderStatsDao: RemoteDictionaryProviderStatsDao,
    private val appPreferences: AppPreferences
) : ViewModel() {
    val providers = remoteDictProviderDao.getAllFlow()

    suspend fun useCustomTabs(): Boolean {
        return appPreferences.get { useCustomTabs }
    }

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