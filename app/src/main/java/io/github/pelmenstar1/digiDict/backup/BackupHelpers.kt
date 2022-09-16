package io.github.pelmenstar1.digiDict.backup

import io.github.pelmenstar1.digiDict.common.unsafeNewArray
import io.github.pelmenstar1.digiDict.data.Record
import io.github.pelmenstar1.digiDict.data.RecordToBadgeRelation
import java.util.*

object BackupHelpers {
    /**
     * Groups given [relations] by [RecordToBadgeRelation.badgeId].
     * [relations] array is expected to be sorted by [RecordToBadgeRelation.badgeId].
     *
     * Time complexity is `n + n * log m` where `n` is [relations] size and `m` is size of records in [recordIdToOrdinalMap]
     */
    fun groupRecordToBadgeRelations(
        relations: Array<out RecordToBadgeRelation>,
        recordIdToOrdinalMap: IdToOrdinalMap
    ): Array<BackupBadgeToMultipleRecordEntry> {
        val relSize = relations.size

        if (relSize == 0) {
            return emptyArray()
        } else {
            val entryCount = countBadgeRegions(relations)
            val entries = unsafeNewArray<BackupBadgeToMultipleRecordEntry>(entryCount)
            var entryIndex = 0

            var regionBadgeId = relations[0].badgeId
            var regionBadgeOrdinal = 0
            var regionStartIndex = 0

            while (regionStartIndex < relSize) {
                val regionEndIndex = findBadgeRegionEnd(relations, regionStartIndex, regionBadgeId)
                val regionLength = regionEndIndex - regionStartIndex
                val recordOrdinals = IntArray(regionLength)

                for (i in 0 until regionLength) {
                    val relation = relations[regionStartIndex + i]

                    recordOrdinals[i] = recordIdToOrdinalMap.getOrdinalById(relation.recordId)
                }

                entries[entryIndex++] = BackupBadgeToMultipleRecordEntry(regionBadgeOrdinal, recordOrdinals)

                regionStartIndex = regionEndIndex
                if (regionEndIndex < relSize) {
                    regionBadgeId = relations[regionEndIndex].badgeId
                }

                regionBadgeOrdinal++
            }

            return entries
        }
    }

    private fun countBadgeRegions(relations: Array<out RecordToBadgeRelation>): Int {
        var count = 0
        var regionBadgeId = relations[0].badgeId

        for (i in 1 until relations.size) {
            val relBadgeId = relations[i].badgeId
            if (regionBadgeId != relBadgeId) {
                count++
                regionBadgeId = relBadgeId
            }
        }

        // +1 to take into account the last region.
        return count + 1
    }

    private fun findBadgeRegionEnd(relations: Array<out RecordToBadgeRelation>, start: Int, badgeId: Int): Int {
        val size = relations.size

        for (i in start until size) {
            if (relations[i].badgeId != badgeId) {
                return i
            }
        }

        return size
    }

    /**
     * Returns whether given [records] contains duplicate expressions.
     * The algorithm is more complex because [records] array is not mutated.
     *
     * Time complexity is around `log (n!)` and memory complexity is `n` where `n` is size of [records]
     */
    fun containsDuplicateExpressions(records: Array<out Record>): Boolean {
        val size = records.size

        val expressions = unsafeNewArray<String>(size)
        var expressionsLength = 0

        for (i in 0 until size) {
            val expr = records[i].expression

            val exprIndex = Arrays.binarySearch(expressions, 0, expressionsLength, expr)
            if (exprIndex >= 0) {
                return true
            } else {
                val insertIndex = -exprIndex - 1

                if (exprIndex != expressionsLength) {
                    System.arraycopy(
                        expressions,
                        insertIndex,
                        expressions,
                        insertIndex + 1,
                        expressionsLength - insertIndex
                    )
                }

                expressions[insertIndex] = expr
                expressionsLength++
            }
        }

        return false
    }
}