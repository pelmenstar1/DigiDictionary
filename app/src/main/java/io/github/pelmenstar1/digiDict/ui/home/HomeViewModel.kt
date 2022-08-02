package io.github.pelmenstar1.digiDict.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.cachedIn
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.pelmenstar1.digiDict.data.AppDatabase
import io.github.pelmenstar1.digiDict.data.ComplexMeaning
import io.github.pelmenstar1.digiDict.utils.FilteredArray
import io.github.pelmenstar1.digiDict.utils.filterFast
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val appDatabase: AppDatabase
) : ViewModel() {
    val items = Pager(
        config = PagingConfig(pageSize = 20),
        pagingSourceFactory = {
            HomePagingSource(appDatabase)
        }
    ).flow.cachedIn(viewModelScope)

    val searchItems = GlobalSearchQueryProvider.queryFlow
        .combine(appDatabase.recordDao().getAllRecordsFlow()) { query, records ->
            if (query.isBlank()) {
                FilteredArray.empty()
            } else {
                records.filterFast {
                    it.expression.startsWith(query, ignoreCase = true) ||
                            ComplexMeaning.anyElementStartsWith(it.rawMeaning, query, ignoreCase = true)
                }
            }
        }.flowOn(Dispatchers.IO)
}