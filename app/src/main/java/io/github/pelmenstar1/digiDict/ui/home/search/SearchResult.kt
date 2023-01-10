package io.github.pelmenstar1.digiDict.ui.home.search

import io.github.pelmenstar1.digiDict.common.FilteredArray
import io.github.pelmenstar1.digiDict.data.ConciseRecordWithBadges

class SearchResult(
    /**
     * Query of the search request.
     */
    val query: String,

    /**
     * Determines whether [query] is meaningful (contains at least one letter or digit)
     */
    val isMeaningfulQuery: Boolean,

    /**
     * The result of the search request.
     *
     * Saved instances of [currentData] should be accessed with great care.
     * After the next [SearchResult], [currentData] will become invalid.
     */
    val currentData: FilteredArray<ConciseRecordWithBadges>,

    /**
     * The result of the previous search request. It can be used to find difference between [currentData] and [previousData].
     * If [previousData] is null, it means it's the first [SearchResult].
     *
     * Saved instances of [previousData] should be accessed with great care.
     * After the next [SearchResult], [previousData] will become invalid.
     */
    val previousData: FilteredArray<ConciseRecordWithBadges>?
)
