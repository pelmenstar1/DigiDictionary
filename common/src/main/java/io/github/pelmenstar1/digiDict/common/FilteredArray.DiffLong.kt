package io.github.pelmenstar1.digiDict.common

import java.util.*

private val DIAGONAL_COMPARATOR = Comparator<DiffDiagonal> { a, b -> a.x - b.x }

class DiffDiagonal(@JvmField val x: Int, @JvmField val y: Int, @JvmField val size: Int)

private class DiffRange {
    @JvmField
    var oldStart = 0

    @JvmField
    var oldEnd = 0

    @JvmField
    var newStart = 0

    @JvmField
    var newEnd = 0

    constructor()
    constructor(oldStart: Int, oldEnd: Int, newStart: Int, newEnd: Int) {
        this.oldStart = oldStart
        this.oldEnd = oldEnd
        this.newStart = newStart
        this.newEnd = newEnd
    }
}

internal class FilteredArrayDiffManagerDelegateLongImpl<T> : FilteredArrayDiffManagerDelegate<T>() {
    private val diagonals = ArrayList<DiffDiagonal>()
    private val cachedSnake = FilteredArrayDiffShared.Snake(0, 0, 0, 0, false)

    override fun calculateDifference(
        oldArray: FilteredArray<out T>,
        newArray: FilteredArray<out T>,
        cb: FilteredArrayDiffItemCallback<T>
    ): FilteredArrayDiffResult {
        val oldSize = oldArray.size
        val newSize = newArray.size

        val oldOrigin = oldArray.origin
        val newOrigin = newArray.origin

        // Relies on the fact that ArrayList saves the buffer on clearing. Instead, it just sets the size to 0
        diagonals.clear()

        // instead of a recursive implementation, we keep our own stack to avoid potential stack
        // overflow exceptions
        val stack = ArrayList<DiffRange>()
        stack.add(DiffRange(0, oldSize, 0, newSize))

        initForwardBackwardArrays(max = (oldSize + newSize + 1) / 2)

        val forward = forwardArray
        val backward = backwardArray

        val snake = cachedSnake

        // We pool the ranges to avoid allocations for each recursive call.
        val rangePool = ArrayList<DiffRange>()

        while (stack.isNotEmpty()) {
            val range = stack.removeAt(stack.size - 1)
            val isValidSnake = FilteredArrayDiffShared.midPointFilteredArray(
                oldOrigin, newOrigin,
                cb,
                range.oldStart, range.oldEnd, range.newStart, range.newEnd,
                forward, backward,
                snake
            )

            if (isValidSnake) {
                // if it has a diagonal, save it
                snake.toDiagonal()?.let {
                    diagonals.add(it)
                }

                // add new ranges for left and right
                val left = if (rangePool.isEmpty()) {
                    DiffRange()
                } else {
                    rangePool.removeAt(rangePool.size - 1)
                }

                left.oldStart = range.oldStart
                left.newStart = range.newStart
                left.oldEnd = snake.startX
                left.newEnd = snake.startY
                stack.add(left)

                // re-use range for right
                range.oldEnd = range.oldEnd
                range.newEnd = range.newEnd
                range.oldStart = snake.endX
                range.newStart = snake.endY
                stack.add(range)
            } else {
                rangePool.add(range)
            }
        }

        // sort snakes
        Collections.sort(diagonals, DIAGONAL_COMPARATOR)

        return createDiffResult(oldArray, newArray, cb, forward.array, diagonals)
    }

    private fun <T> createDiffResult(
        oldArray: FilteredArray<out T>,
        newArray: FilteredArray<out T>,
        cb: FilteredArrayDiffItemCallback<T>,
        statuses: IntArray,
        diagonals: ArrayList<DiffDiagonal>
    ): FilteredArrayDiffResult {
        return FilteredArrayDiffShared.createDiffResult(
            oldArray, newArray, cb, statuses, diagonals,
            ArrayList<DiffDiagonal>::size, ArrayList<DiffDiagonal>::get,
            DiffDiagonal::x, DiffDiagonal::y, DiffDiagonal::size,
            addEdgeDiagonals = {
                val first = diagonals.firstOrNull()

                if (first == null || first.x != 0 || first.y != 0) {
                    diagonals.add(0, DiffDiagonal(0, 0, 0))
                }

                diagonals.add(DiffDiagonal(oldArray.size, newArray.size, 0))
            }
        )
    }
}