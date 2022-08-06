package io.github.pelmenstar1.digiDict.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.cachedIn
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.pelmenstar1.digiDict.data.AppDatabase
import io.github.pelmenstar1.digiDict.utils.FilteredArray
import io.github.pelmenstar1.digiDict.utils.LocaleProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combineTransform
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val appDatabase: AppDatabase,
    localeProvider: LocaleProvider
) : ViewModel() {
    val items = Pager(
        config = PagingConfig(pageSize = 20),
        pagingSourceFactory = {
            HomePagingSource(appDatabase)
        }
    ).flow.cachedIn(viewModelScope)

    private val locale = localeProvider.get()

    val searchItems = combineTransform(
        GlobalSearchQueryProvider.queryFlow,
        appDatabase.recordDao().getAllRecordsOrderByIdFlow(),
        appDatabase.searchPreparedRecordDao().getAllOrderByIdFlow()
    ) { query, records, searchRecords ->
        val result = if (query.isBlank()) {
            FilteredArray.empty()
        } else {
            RecordSearchUtil.filter(records, searchRecords, query, locale)
        }

        emit(result)
    }.flowOn(Dispatchers.IO).stateIn(viewModelScope, SharingStarted.Lazily, FilteredArray.empty())
}