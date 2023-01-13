package io.github.pelmenstar1.digiDict.ui.home.search

import io.github.pelmenstar1.digiDict.common.*
import io.github.pelmenstar1.digiDict.data.ConciseRecordWithBadges
import io.github.pelmenstar1.digiDict.data.HomeSortType
import io.github.pelmenstar1.digiDict.data.getComparatorForConciseRecordWithBadges

class HomeSearchManager {
    private var _previousRecords: Array<out ConciseRecordWithBadges>? = null
    private var _currentRecords: Array<out ConciseRecordWithBadges>? = null

    private var _previousResultBitSet: LongArray? = null
    private var _currentResultBitSet: LongArray? = null

    private var _previousPostBitSetMap: IntArray? = null

    var currentRecords: Array<out ConciseRecordWithBadges>
        get() = requireNotNull(_currentRecords)
        set(value) {
            _currentRecords = value
        }

    fun onSearchRequest(query: String, sortType: HomeSortType): SearchResult {
        val data = currentRecords

        var prevBitSet = _previousResultBitSet
        var currentBitSet = _currentResultBitSet

        val expectedBitSetSize = ArrayFilterHelpers.calculateBitSetSize(data.size)

        if (currentBitSet == null) {
            // If _currentResultBitSet is null, it means _previousResultBitSet is null as well.
            // We'll need prevBitSet initialized anyway, so do it right now.
            prevBitSet = LongArray(expectedBitSetSize)
            currentBitSet = LongArray(expectedBitSetSize)

            _previousResultBitSet = prevBitSet
            _currentResultBitSet = currentBitSet
        } else if (currentBitSet.size != expectedBitSetSize) {
            currentBitSet = currentBitSet.copyOf(expectedBitSetSize)
            _currentResultBitSet = currentBitSet
        }

        // It's to make Kotlin compiler sure that prevBitSet is not null. It's actually is but the compiler doesn't trust
        // the code
        prevBitSet!!

        // There can be such case that prevBitSet is smaller than currentBitSet.
        // For example, when records property is updated to a bigger array, _previousResultBitSet's size remains the same
        // and then currentBitSet size can be actually bigger than prevBitSet because underlying "current" array is bigger.
        if (prevBitSet.size < currentBitSet.size) {
            // Resize the array then but don't copy the content because we'll move the content
            // of currentBitSet to prevBitSet below.
            prevBitSet = LongArray(currentBitSet.size)

            _previousResultBitSet = prevBitSet
        }

        // Move the content of currentBitSet to prevBitSet as currentBitSet is actually "previous".
        System.arraycopy(currentBitSet, 0, prevBitSet, 0, currentBitSet.size)

        val isMeaningfulQuery = query.containsLetterOrDigit()

        // Search can only be performed if query contains at least one letter or digit.
        // Otherwise, there's no sense in it as both expression and meaning should have at least one letter or digit.
        val currentFilteredArray = if (isMeaningfulQuery) {
            val preparedQuery = RecordSearchUtil.prepareQuery(query)
            val unsortedResult = data.toFilteredArray(currentBitSet) { record ->
                RecordSearchUtil.filterPredicate(record, preparedQuery)
            }

            unsortedResult.sorted(sortType.getComparatorForConciseRecordWithBadges())
        } else {
            FilteredArray.empty()
        }

        val prevFilteredArray = _previousRecords?.let {
            FilteredArray.createUnsafe(it, prevBitSet, _previousPostBitSetMap)
        }

        _previousRecords = data
        _previousPostBitSetMap = currentFilteredArray.postBitSetMap

        return SearchResult(query, isMeaningfulQuery, currentFilteredArray, prevFilteredArray)
    }
}