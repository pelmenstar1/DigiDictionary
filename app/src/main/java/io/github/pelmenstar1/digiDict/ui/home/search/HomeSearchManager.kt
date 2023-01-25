package io.github.pelmenstar1.digiDict.ui.home.search

import io.github.pelmenstar1.digiDict.common.*
import io.github.pelmenstar1.digiDict.data.ConciseRecordWithBadges
import io.github.pelmenstar1.digiDict.data.HomeSortType
import io.github.pelmenstar1.digiDict.data.getComparatorForConciseRecordWithBadges

class HomeSearchManager(private val core: HomeSearchCore) {
    // _previousRecords saves the reference to _currentRecords of the previous search request. It's
    // needed to correctly handle the situation when _currentRecords is changed. Where-as _previousSavedRecords
    // saves the result of previous search request.

    private var _previousRecords: Array<out ConciseRecordWithBadges>? = null
    private var _previousSavedRecords: Array<out ConciseRecordWithBadges>? = null
    private var _currentRecords: Array<out ConciseRecordWithBadges>? = null
    private var _currentRecordsSize = 0

    var currentRecords: Array<out ConciseRecordWithBadges>
        get() = requireNotNull(_currentRecords)
        set(value) {
            _currentRecords = value
        }

    fun onSearchRequest(query: String, sortType: HomeSortType): HomeSearchResult {
        val currentRecords = currentRecords
        var prevSavedRecords = _previousSavedRecords

        // _currentRecordsSize is actually previous by this moment.
        val prevRecordsSize = _currentRecordsSize

        if (prevSavedRecords == null || prevRecordsSize > prevSavedRecords.size) {
            prevSavedRecords = unsafeNewArray(prevRecordsSize)
            _previousSavedRecords = prevSavedRecords
        }

        _previousRecords?.let {
            System.arraycopy(it, 0, prevSavedRecords, 0, prevRecordsSize)
        }

        val isMeaningfulQuery = query.containsLetterOrDigit()

        val currentFilteredArray = if (isMeaningfulQuery) {
            val preparedQuery = core.prepareQuery(query)

            currentRecords.toFilteredArray { record ->
                core.filterPredicate(record, preparedQuery)
            }.also {
                it.sort(sortType.getComparatorForConciseRecordWithBadges())
            }
        } else {
            FilteredArray.empty()
        }

        val prevFilteredArray = FilteredArray(prevSavedRecords, prevRecordsSize)
        _currentRecordsSize = currentFilteredArray.size
        _previousRecords = currentRecords

        return HomeSearchResult(query, isMeaningfulQuery, currentFilteredArray, prevFilteredArray)
    }
}