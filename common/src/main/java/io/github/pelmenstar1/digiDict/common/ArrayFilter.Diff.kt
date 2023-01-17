package io.github.pelmenstar1.digiDict.common

import java.util.*
import kotlin.math.max

interface FilteredArrayDiffResult {
    fun dispatchTo(callback: ListUpdateCallback)
}

interface FilteredArrayDiffItemCallback<in T> {
    fun areItemsTheSame(a: T, b: T): Boolean
    fun areContentsTheSame(a: T, b: T): Boolean
}

internal sealed class FilteredArrayDiffManagerDelegate<T> {
    // K lines are diagonal lines in the matrix. (see the paper for details)
    // These arrays lines keep the max reachable position for each k-line.
    protected var forwardArray = ArrayFilterDiffShared.CenteredIntArray(EmptyArray.INT)
    protected var backwardArray = ArrayFilterDiffShared.CenteredIntArray(EmptyArray.INT)

    abstract fun calculateDifference(
        oldArray: FilteredArray<out T>,
        newArray: FilteredArray<out T>,
        cb: FilteredArrayDiffItemCallback<T>
    ): FilteredArrayDiffResult

    protected fun initForwardBackwardArrays(max: Int) {
        val forward = forwardArray
        val backward = backwardArray

        val linesLength = max * 2 + 1

        if (linesLength >= forward.size) {
            forwardArray = ArrayFilterDiffShared.CenteredIntArray(linesLength)
            backwardArray = ArrayFilterDiffShared.CenteredIntArray(linesLength)
        } else {
            // Fill with zero only those parts we'll need
            Arrays.fill(forward.array, 0, linesLength, 0)
            Arrays.fill(backward.array, 0, linesLength, 0)
        }
    }
}

class FilteredArrayDiffManager<T>(private val itemCallback: FilteredArrayDiffItemCallback<T>) {
    internal var delegate: FilteredArrayDiffManagerDelegate<T>? = null

    private fun useShortImpl(oldSize: Int, newSize: Int): Boolean {
        return max(oldSize, newSize) < 0xFFFF
    }

    internal fun resolveDelegate(oldSize: Int, newSize: Int): FilteredArrayDiffManagerDelegate<T> {
        var currentDelegate = delegate
        if (currentDelegate == null) {
            currentDelegate = if (useShortImpl(oldSize, newSize)) {
                FilteredArrayDiffManagerDelegateShortImpl()
            } else {
                FilteredArrayDiffManagerDelegateLongImpl()
            }

            delegate = currentDelegate
        } else {
            // We don't switch to short-impl when it's actually possible to prevent re-allocating caches too much.
            // We only switch to long-impl when it's necessary
            if (currentDelegate is FilteredArrayDiffManagerDelegateShortImpl && !useShortImpl(oldSize, newSize)) {
                currentDelegate = FilteredArrayDiffManagerDelegateLongImpl()

                delegate = currentDelegate
            }
        }

        return currentDelegate
    }

    fun calculateDifference(oldArray: FilteredArray<out T>, newArray: FilteredArray<out T>): FilteredArrayDiffResult {
        val currentDelegate = resolveDelegate(oldArray.size, newArray.size)

        return currentDelegate.calculateDifference(oldArray, newArray, itemCallback)
    }
}