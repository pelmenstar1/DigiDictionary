@file:Suppress("NOTHING_TO_INLINE")

package io.github.pelmenstar1.digiDict.data

import java.util.*

fun PackedRecordToBadgeRelation(recordId: Int, badgeId: Int): PackedRecordToBadgeRelation {
    val packed = (recordId.toLong() and 0xFFFFFFFFL) or (badgeId.toLong() shl 32)

    return PackedRecordToBadgeRelation(packed)
}

/**
 * Represents [RecordToBadgeRelation] packed into a [Long]. But it doesn't have relation id field.
 */
@JvmInline
value class PackedRecordToBadgeRelation(@JvmField val packed: Long) {
    inline val recordId: Int
        get() = (packed and 0xFFFFFFFFL).toInt()

    inline val badgeId: Int
        get() = (packed shr 32).toInt()

    override fun toString(): String {
        return "PackedRecordToBadgeRelation(recordId=$recordId, badgeId=$badgeId)"
    }
}

inline fun PackedRecordToBadgeRelationArray(size: Int): PackedRecordToBadgeRelationArray {
    return PackedRecordToBadgeRelationArray(LongArray(size))
}

@JvmInline
value class PackedRecordToBadgeRelationArray(@JvmField val array: LongArray) {
    inline val size: Int
        get() = array.size

    inline operator fun get(index: Int): PackedRecordToBadgeRelation {
        return PackedRecordToBadgeRelation(array[index])
    }

    inline operator fun set(index: Int, value: PackedRecordToBadgeRelation) {
        array[index] = value.packed
    }

    /**
     * Finds an index of the first relation with specified [recordId]. If it's not found, returns -1.
     */
    fun binarySearchRecordId(recordId: Int): Int {
        var low = 0
        var high = array.size - 1

        while (low <= high) {
            val mid = (low + high) ushr 1
            val midVal = get(mid).recordId

            if (midVal < recordId) {
                low = mid + 1
            } else if (midVal > recordId) {
                high = mid - 1
            } else {
                return mid
            }
        }

        return -1
    }
}