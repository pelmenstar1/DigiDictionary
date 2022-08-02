package io.github.pelmenstar1.digiDict.ui.chooseRemoteDictProvider

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.pelmenstar1.digiDict.data.RemoteDictionaryProviderDao
import io.github.pelmenstar1.digiDict.data.RemoteDictionaryProviderInfo
import io.github.pelmenstar1.digiDict.data.RemoteDictionaryProviderStatsDao
import io.github.pelmenstar1.digiDict.prefs.AppPreferences
import io.github.pelmenstar1.digiDict.prefs.get
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ChooseRemoteDictionaryProviderViewModel @Inject constructor(
    private val remoteDictProviderDao: RemoteDictionaryProviderDao,
    private val remoteDictProviderStatsDao: RemoteDictionaryProviderStatsDao,
    private val appPreferences: AppPreferences
) : ViewModel() {
    val providers = remoteDictProviderDao.getAllFlow()

    suspend fun useCustomTabs() = appPreferences.get { useCustomTabs }
    suspend fun getMostUsedProviders() = remoteDictProviderDao.getMostUsedProviders()

    fun onRemoteDictionaryProviderUsed(provider: RemoteDictionaryProviderInfo) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                remoteDictProviderStatsDao.incrementVisitCount(provider.id)
            } catch (e: Exception) {
                Log.e(TAG, "incrementVisitCount()", e)
            }
        }
    }

    companion object {
        private const val TAG = "ChooseRDP_VM"
    }
}