package io.github.pelmenstar1.digiDict.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.cachedIn
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.pelmenstar1.digiDict.common.*
import io.github.pelmenstar1.digiDict.common.android.onDatabaseTablesUpdated
import io.github.pelmenstar1.digiDict.data.AppDatabase
import io.github.pelmenstar1.digiDict.data.HomeSortType
import io.github.pelmenstar1.digiDict.data.getAllConciseRecordsWithBadges
import io.github.pelmenstar1.digiDict.data.getComparatorForConciseRecordWithBadges
import io.github.pelmenstar1.digiDict.ui.home.search.GlobalSearchQueryProvider
import io.github.pelmenstar1.digiDict.ui.home.search.RecordSearchUtil
import io.github.pelmenstar1.digiDict.ui.home.search.SearchResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val appDatabase: AppDatabase
) : ViewModel() {
    private val _sortTypeFlow = MutableStateFlow(HomeSortType.NEWEST)
    val sortTypeFlow: StateFlow<HomeSortType>
        get() = _sortTypeFlow

    var sortType: HomeSortType
        get() = _sortTypeFlow.value
        set(value) {
            _sortTypeFlow.value = value
        }

    val items = Pager(
        config = PagingConfig(pageSize = 20),
        pagingSourceFactory = {
            HomePagingSource(appDatabase, sortType)
        }
    ).flow.cachedIn(viewModelScope)

    private val searchProgressReporter = ProgressReporter()
    val searchProgressFlow = searchProgressReporter.progressFlow

    private val searchStateManager = DataLoadStateManager<SearchResult>(TAG)

    val searchStateFlow = searchStateManager.buildFlow(viewModelScope) {
        fromFlow {
            // This makes getAllConciseRecordsWithSearchInfoAndBadges() being invoked once
            // value of isActiveFlow is true. When value is changed from true to false,
            // filterTrue() will prevent false to trigger collection.
            val recordFlow = GlobalSearchQueryProvider
                .isActiveFlow
                .filterTrue()
                .distinctUntilChanged()
                .map {
                    trackProgressWith(searchProgressReporter) {
                        appDatabase.getAllConciseRecordsWithBadges(searchProgressReporter)
                    }
                }

            combine(
                GlobalSearchQueryProvider.queryFlow, recordFlow, _sortTypeFlow
            ) { query, records, sortType ->
                val isMeaningfulQuery = query.containsLetterOrDigit()

                // Search can only be performed if query contains at least one letter or digit.
                // Otherwise, there's no sense in it because such expressions and meanings are forbidden
                val result = if (isMeaningfulQuery) {
                    RecordSearchUtil
                        .filter(records, query)
                        .sorted(sortType.getComparatorForConciseRecordWithBadges())
                } else {
                    FilteredArray.empty()
                }

                SearchResult(query, isMeaningfulQuery, result)
            }.flowOn(Dispatchers.Default)
        }
    }

    init {
        onDatabaseTablesUpdated(
            appDatabase,
            tables = arrayOf("records", "record_badges", "record_to_badge_relations")
        ) {
            retrySearch()
        }
    }

    fun retrySearch() {
        searchStateManager.retry()
    }

    companion object {
        private const val TAG = "HomeViewModel"
    }
}