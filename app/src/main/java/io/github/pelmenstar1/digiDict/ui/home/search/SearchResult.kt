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
     */
    val data: FilteredArray<ConciseRecordWithBadges>
)
