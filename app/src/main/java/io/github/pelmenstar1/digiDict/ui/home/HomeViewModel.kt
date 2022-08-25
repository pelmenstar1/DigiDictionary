package io.github.pelmenstar1.digiDict.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.cachedIn
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.pelmenstar1.digiDict.common.*
import io.github.pelmenstar1.digiDict.data.AppDatabase
import io.github.pelmenstar1.digiDict.data.ConciseRecordWithSearchInfoAndBadges
import io.github.pelmenstar1.digiDict.ui.home.search.GlobalSearchQueryProvider
import io.github.pelmenstar1.digiDict.ui.home.search.RecordSearchUtil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.flowOn
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val appDatabase: AppDatabase,
    localeProvider: LocaleProvider
) : ViewModel() {
    private val locale = localeProvider.get()

    val items = Pager(
        config = PagingConfig(pageSize = 20),
        pagingSourceFactory = {
            HomePagingSource(appDatabase)
        }
    ).flow.cachedIn(viewModelScope)

    private val recordDao = appDatabase.recordDao()
    private val searchUpdateFlow = databaseObserverFlow(
        appDatabase,
        tables = arrayOf("records", "record_badges", "record_to_badge_relations")
    )

    private val searchStateManager = DataLoadStateManager<FilteredArray<ConciseRecordWithSearchInfoAndBadges>>(TAG)
    val searchStateFlow = searchStateManager.buildFlow(viewModelScope) {
        fromFlow {
            // This makes getAllRecordsWithSearchInfoFlow() to be collected once
            // value of isActiveFlow is true. When value is changed from true to false,
            // filter { it } will prevent false to trigger creation and collection of getAllRecordsWithSearchInfoFlow()
            val recordFlow = GlobalSearchQueryProvider
                .isActiveFlow
                .filter { it }
                .combine(searchUpdateFlow) { _, _ ->
                    recordDao.getAllConciseRecordsWithSearchInfoAndBadges()
                }

            recordFlow.combine(GlobalSearchQueryProvider.queryFlow) { records, query ->
                // Search can only be performed if query contains at least one letter or digit.
                // Otherwise, there's no sense in it.
                if (query.containsLetterOrDigit()) {
                    RecordSearchUtil.filter(records, query, locale)
                } else {
                    FilteredArray.empty()
                }
            }.flowOn(Dispatchers.IO)
        }
    }

    fun retrySearch() {
        searchStateManager.retry()
    }

    companion object {
        private const val TAG = "HomeViewModel"
    }
}