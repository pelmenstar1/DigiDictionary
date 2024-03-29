package io.github.pelmenstar1.digiDict.search

import io.github.pelmenstar1.digiDict.common.FilteredArray
import io.github.pelmenstar1.digiDict.data.ConciseRecordWithBadges

class RecordSearchResult(
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
     * After the next [RecordSearchResult], [currentData] will become invalid.
     */
    val currentData: FilteredArray<out ConciseRecordWithBadges>,

    /**
     * The result of the previous search request. It can be used to find difference between [currentData] and [previousData].
     * If it's the first request, the array will be empty.
     *
     * Saved instances of [previousData] should be accessed with great care.
     * After the next [RecordSearchResult], [previousData] will become invalid.
     */
    val previousData: FilteredArray<out ConciseRecordWithBadges>,

    /**
     * A metadata provider for this search result.
     */
    val metadataProvider: RecordSearchMetadataProvider
)
