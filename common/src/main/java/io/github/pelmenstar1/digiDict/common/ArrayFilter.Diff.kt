package io.github.pelmenstar1.digiDict.common

import kotlin.math.max

interface FilteredArrayDiffResult {
    fun dispatchTo(callback: ListUpdateCallback)
}

interface FilteredArrayDiffItemCallback<in T> {
    fun areItemsTheSame(a: T, b: T): Boolean
    fun areContentsTheSame(a: T, b: T): Boolean
}

// Exclusive bound
private const val SHORT_IMPL_UPPER_BOUND = 0xFFFF

fun <T> FilteredArray<out T>.calculateDifference(
    newArray: FilteredArray<out T>,
    cb: FilteredArrayDiffItemCallback<T>
): FilteredArrayDiffResult {
    return if (max(size, newArray.size) < SHORT_IMPL_UPPER_BOUND) {
        ArrayFilterDiffShort.calculateDifference(this, newArray, cb)
    } else {
        ArrayFilterDiffLong.calculateDifference(this, newArray, cb)
    }
}