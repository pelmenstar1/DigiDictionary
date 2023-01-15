package io.github.pelmenstar1.digiDict.ui.home.search

import io.github.pelmenstar1.digiDict.common.*
import io.github.pelmenstar1.digiDict.data.ConciseRecordWithBadges
import io.github.pelmenstar1.digiDict.data.HomeSortType
import io.github.pelmenstar1.digiDict.data.getComparatorForConciseRecordWithBadges

class HomeSearchManager {
    private var _previousRecords: Array<out ConciseRecordWithBadges>? = null
    private var _currentRecords: Array<out ConciseRecordWithBadges>? = null
    private var _currentRecordsSize = 0

    var currentRecords: Array<out ConciseRecordWithBadges>
        get() = requireNotNull(_currentRecords)
        set(value) {
            _currentRecords = value
        }

    fun onSearchRequest(query: String, sortType: HomeSortType): SearchResult {
        val currentRecords = currentRecords

        var prevRecords = _previousRecords

        // _currentRecordsSize is actually previous by this moment.
        val prevRecordsSize = _currentRecordsSize

        if (prevRecordsSize > 0) {
            if (prevRecords == null || prevRecordsSize > prevRecords.size) {
                prevRecords = unsafeNewArray(prevRecordsSize)
                _previousRecords = prevRecords
            }

            System.arraycopy(currentRecords, 0, prevRecords, 0, prevRecordsSize)
        }

        val isMeaningfulQuery = query.containsLetterOrDigit()

        val currentFilteredArray = if (isMeaningfulQuery) {
            currentRecords.toFilteredArray { record ->
                RecordSearchUtil.filterPredicate(record, query)
            }.sorted(sortType.getComparatorForConciseRecordWithBadges())
        } else {
            FilteredArray.empty()
        }

        val prevFilteredArray = prevRecords?.let { FilteredArray(it, prevRecordsSize) }
        _currentRecordsSize = currentFilteredArray.size

        return SearchResult(query, isMeaningfulQuery, currentFilteredArray, prevFilteredArray)
    }
}