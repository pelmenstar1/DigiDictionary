package io.github.pelmenstar1.digiDict.ui.manageRemoteDictProviders

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.pelmenstar1.digiDict.data.AppDatabase
import io.github.pelmenstar1.digiDict.data.RemoteDictionaryProviderInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ManageRemoteDictionaryProvidersViewModel @Inject constructor(
    appDatabase: AppDatabase
) : ViewModel() {
    private val remoteDictProviderDao = appDatabase.remoteDictionaryProviderDao()

    val providersFlow = remoteDictProviderDao.getAllFlow()

    fun delete(provider: RemoteDictionaryProviderInfo) {
        viewModelScope.launch(Dispatchers.Default) {
            remoteDictProviderDao.delete(provider)
        }
    }
}