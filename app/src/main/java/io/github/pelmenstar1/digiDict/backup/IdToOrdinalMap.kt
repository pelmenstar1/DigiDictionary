package io.github.pelmenstar1.digiDict.backup

import io.github.pelmenstar1.digiDict.common.mapToIntArray
import io.github.pelmenstar1.digiDict.data.EntityWithPrimaryKeyId

/**
 * Represents a simple map which is able to transform entity id to its ordinal number and vise versa.
 */
class IdToOrdinalMap {
    internal val sortedIds: IntArray

    constructor(values: Array<out EntityWithPrimaryKeyId>) {
        sortedIds = values.mapToIntArray { it.id }
    }

    constructor(sortedIds: IntArray) {
        this.sortedIds = sortedIds
    }

    fun getOrdinalById(id: Int): Int {
        return sortedIds.binarySearch(id)
    }
}