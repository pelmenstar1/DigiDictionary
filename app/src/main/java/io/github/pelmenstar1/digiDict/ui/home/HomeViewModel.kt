package io.github.pelmenstar1.digiDict.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavController
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.cachedIn
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.pelmenstar1.digiDict.data.AppDatabase
import io.github.pelmenstar1.digiDict.ui.home.HomePagingSource
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val appDatabase: AppDatabase
) : ViewModel() {
    private val dao = appDatabase.recordDao()

    val items = Pager(
        config = PagingConfig(pageSize = 20),
        pagingSourceFactory = {
            HomePagingSource(appDatabase)
        }
    ).flow.cachedIn(viewModelScope)

    fun navigateToAddRecord(navController: NavController) {
        navController.navigate(HomeFragmentDirections.actionHomeToAddEditRecord())
    }
}