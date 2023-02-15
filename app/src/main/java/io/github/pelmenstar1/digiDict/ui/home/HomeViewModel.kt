package io.github.pelmenstar1.digiDict.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.cachedIn
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.pelmenstar1.digiDict.common.DataLoadStateManager
import io.github.pelmenstar1.digiDict.common.ProgressReporter
import io.github.pelmenstar1.digiDict.common.android.onDatabaseTablesUpdated
import io.github.pelmenstar1.digiDict.common.filterTrue
import io.github.pelmenstar1.digiDict.common.trackProgressWith
import io.github.pelmenstar1.digiDict.data.AppDatabase
import io.github.pelmenstar1.digiDict.data.RecordSortType
import io.github.pelmenstar1.digiDict.data.getAllConciseRecordsWithBadges
import io.github.pelmenstar1.digiDict.search.*
import io.github.pelmenstar1.digiDict.ui.home.search.GlobalSearchQueryProvider
import io.github.pelmenstar1.digiDict.ui.paging.AppPagingSource
import io.github.pelmenstar1.digiDict.ui.record.RecordTextPrecomputeController
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val appDatabase: AppDatabase,
    searchCore: RecordSearchCore
) : ViewModel() {
    private val _sortTypeFlow = MutableStateFlow(RecordSortType.NEWEST)
    val sortTypeFlow: StateFlow<RecordSortType>
        get() = _sortTypeFlow

    private val _searchPropertiesFlow = MutableStateFlow<Array<out RecordSearchProperty>>(RecordSearchProperty.values())

    val searchPropertiesFlow: StateFlow<Array<out RecordSearchProperty>>
        get() = _searchPropertiesFlow

    var searchProperties: Array<out RecordSearchProperty>
        get() = _searchPropertiesFlow.value
        set(value) {
            _searchPropertiesFlow.value = value
        }

    var sortType: RecordSortType
        get() = _sortTypeFlow.value
        set(value) {
            _sortTypeFlow.value = value
        }


    /**
     * Gets or sets [RecordTextPrecomputeController] of the view-model.
     *
     * By the time of collecting [items] flow, the [recordTextPrecomputeController] value should be non-null.
     */
    var recordTextPrecomputeController: RecordTextPrecomputeController? = null

    val items = Pager(
        config = PagingConfig(pageSize = 20, enablePlaceholders = false),
        pagingSourceFactory = {
            AppPagingSource(
                appDatabase,
                sortType,
                recordTextPrecomputeController!!
            )
        }
    ).flow.cachedIn(viewModelScope)

    private val searchManager = RecordSearchManager(searchCore)
    private val searchOptionsFlow = _searchPropertiesFlow.map {
        RecordSearchOptions(it)
    }

    private val searchProgressReporter = ProgressReporter()
    val searchProgressFlow = searchProgressReporter.progressFlow

    private val searchStateManager = DataLoadStateManager<RecordSearchResult>(TAG)

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
                }.onEach {
                    searchManager.currentRecords = it
                }

            combine(
                GlobalSearchQueryProvider.queryFlow, _sortTypeFlow, searchOptionsFlow, recordFlow
            ) { query, sortType, options, _ ->
                searchManager.onSearchRequest(query, sortType, options)
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