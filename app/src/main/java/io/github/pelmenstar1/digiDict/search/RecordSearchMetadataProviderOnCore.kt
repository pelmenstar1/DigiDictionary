package io.github.pelmenstar1.digiDict.search

import io.github.pelmenstar1.digiDict.common.IntList
import io.github.pelmenstar1.digiDict.data.ConciseRecordWithBadges

/**
 * Implementation of [RecordSearchMetadataProvider] based on [RecordSearchCore].
 *
 * @param core currently used search core
 * @param query a query of the current search request. It must be "prepared"
 * @param options an [RecordSearchOptions] instance with which the request was processed
 */
class RecordSearchMetadataProviderOnCore(
    private val core: RecordSearchCore,
    private val query: String,
    private val options: RecordSearchOptions
) : RecordSearchMetadataProvider {
    private val dataList = IntList(16)

    override fun calculateFoundRanges(record: ConciseRecordWithBadges): IntArray {
        dataList.size = 0
        core.calculateFoundRanges(record, query, options, dataList)

        return dataList.toArray()
    }
}