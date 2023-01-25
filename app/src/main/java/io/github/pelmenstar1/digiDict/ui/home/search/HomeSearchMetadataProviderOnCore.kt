package io.github.pelmenstar1.digiDict.ui.home.search

import io.github.pelmenstar1.digiDict.common.IntList
import io.github.pelmenstar1.digiDict.data.ConciseRecordWithBadges

/**
 * Implementation of [HomeSearchMetadataProvider] based on [HomeSearchCore].
 */
class HomeSearchMetadataProviderOnCore(private val core: HomeSearchCore) : HomeSearchMetadataProvider {
    private val dataList = IntList(16)
    private var query = ""

    override fun onQueryChanged(value: String) {
        query = core.prepareQuery(value)
    }

    override fun calculateFoundRanges(record: ConciseRecordWithBadges): IntArray {
        dataList.size = 0
        core.calculateFoundRanges(record, query, dataList)

        return dataList.toArray()
    }
}