package io.github.pelmenstar1.digiDict.ui.home

import androidx.lifecycle.SavedStateHandle
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
import io.github.pelmenstar1.digiDict.data.getAllSortedPackedRecordToBadgeRelations
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
    searchCore: RecordSearchCore,
    private val savedStateHandle: SavedStateHandle
) : ViewModel() {
    val sortTypeFlow = savedStateHandle.getStateFlow(KEY_SORT_TYPE, RecordSortType.NEWEST)
    val searchPropertiesFlow = savedStateHandle.getStateFlow(KEY_SEARCH_PROPERTIES, RecordSearchPropertySet.all())

    var searchProperties: RecordSearchPropertySet
        get() = searchPropertiesFlow.value
        set(value) {
            savedStateHandle[KEY_SEARCH_PROPERTIES] = value
        }

    var sortType: RecordSortType
        get() = sortTypeFlow.value
        set(value) {
            savedStateHandle[KEY_SORT_TYPE] = value
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
    private val searchOptionsFlow = searchPropertiesFlow.map {
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
                        val relations = appDatabase.getAllSortedPackedRecordToBadgeRelations()

                        appDatabase.getAllConciseRecordsWithBadges(relations, searchProgressReporter)
                    }
                }.onEach {
                    searchManager.currentRecords = it
                }

            combine(
                GlobalSearchQueryProvider.queryFlow, sortTypeFlow, searchOptionsFlow, recordFlow
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

        private const val KEY_SORT_TYPE = "io.github.pelmenstar1.digiDict.HomeViewModel.sortType"
        private const val KEY_SEARCH_PROPERTIES = "io.github.pelmenstar1.digiDict.HomeViewModel.searchProperties"
    }
}