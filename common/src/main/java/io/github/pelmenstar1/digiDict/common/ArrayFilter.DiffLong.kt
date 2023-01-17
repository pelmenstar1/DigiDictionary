package io.github.pelmenstar1.digiDict.common

import java.util.*
import kotlin.Comparator
import kotlin.collections.ArrayList

object ArrayFilterDiffLong {
    private val DIAGONAL_COMPARATOR = Comparator<Diagonal> { a, b -> a.x - b.x }

    class Diagonal(@JvmField val x: Int, @JvmField val y: Int, @JvmField val size: Int)

    private class Range {
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

    internal fun <T> calculateDifference(
        oldArray: FilteredArray<out T>,
        newArray: FilteredArray<out T>,
        cb: FilteredArrayDiffItemCallback<T>
    ): FilteredArrayDiffResult {
        val oldSize = oldArray.size
        val newSize = newArray.size

        val oldOrigin = oldArray.origin
        val newOrigin = newArray.origin

        val diagonals = ArrayList<Diagonal>()

        // instead of a recursive implementation, we keep our own stack to avoid potential stack
        // overflow exceptions
        val stack = ArrayList<Range>()
        stack.add(Range(0, oldSize, 0, newSize))

        val max = (oldSize + newSize + 1) / 2

        // allocate forward and backward k-lines. K lines are diagonal lines in the matrix. (see the
        // paper for details)
        // These arrays lines keep the max reachable position for each k-line.
        val forward = ArrayFilterDiffShared.CenteredIntArray(max * 2 + 1)
        val backward = ArrayFilterDiffShared.CenteredIntArray(max * 2 + 1)

        // We pool the ranges to avoid allocations for each recursive call.
        val rangePool = ArrayList<Range>()
        while (stack.isNotEmpty()) {
            val range = stack.removeAt(stack.size - 1)
            val snake = ArrayFilterDiffShared.midPointFilteredArray(
                oldOrigin, newOrigin,
                cb,
                range.oldStart, range.oldEnd, range.newStart, range.newEnd,
                forward, backward
            )

            if (snake != null) {
                // if it has a diagonal, save it
                snake.toDiagonal()?.let {
                    diagonals.add(it)
                }

                // add new ranges for left and right
                val left = if (rangePool.isEmpty()) {
                    Range()
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
        diagonals.sortWith(DIAGONAL_COMPARATOR)

        return createDiffResult(oldArray, newArray, cb, forward.array, diagonals)
    }

    private fun <T> createDiffResult(
        oldArray: FilteredArray<out T>,
        newArray: FilteredArray<out T>,
        cb: FilteredArrayDiffItemCallback<T>,
        statuses: IntArray,
        diagonals: ArrayList<Diagonal>
    ): FilteredArrayDiffResult {
        return ArrayFilterDiffShared.createDiffResult(
            oldArray, newArray, cb, statuses, diagonals,
            ArrayList<Diagonal>::size, ArrayList<Diagonal>::get,
            Diagonal::x, Diagonal::y, Diagonal::size,
            addEdgeDiagonals = {
                val first = diagonals.firstOrNull()

                if (first == null || first.x != 0 || first.y != 0) {
                    diagonals.add(0, Diagonal(0, 0, 0))
                }

                diagonals.add(Diagonal(oldArray.size, newArray.size, 0))
            }
        )
    }
}