package io.github.pelmenstar1.digiDict.common

import java.util.*
import kotlin.math.abs
import kotlin.math.min

internal object ArrayFilterDiffShared {
    class Snake(
        @JvmField var startX: Int,
        @JvmField var startY: Int,
        @JvmField var endX: Int,
        @JvmField var endY: Int,
        @JvmField var isReversed: Boolean
    ) {
        fun toDiagonal() = toDiagonalInternal(ArrayFilterDiffLong::Diagonal, noneValue = { null })
        fun toPackedDiagonal() = toDiagonalInternal(
            ArrayFilterDiffShort::PackedDiagonal,
            noneValue = { ArrayFilterDiffShort.PackedDiagonal.NONE }
        )

        private inline fun <T> toDiagonalInternal(createDiagonal: (x: Int, y: Int, size: Int) -> T, noneValue: () -> T): T {
            val sx = startX
            val sy = startY
            val ex = endX
            val ey = endY

            val xDiff = ex - sx
            val yDiff = ey - sy

            val dSize = min(xDiff, yDiff)

            return if (dSize > 0) {
                // it's either an addition or a removal
                if (xDiff != yDiff) {
                    if (isReversed) {
                        // snake edge it at the end
                        createDiagonal(sx, sy, dSize)
                    } else {
                        // snake edge it at the beginning
                        if (yDiff > xDiff) {
                            createDiagonal(sx, sy + 1, dSize)
                        } else {
                            createDiagonal(sx + 1, sy, dSize)
                        }
                    }
                } else {
                    // we are a pure diagonal
                    createDiagonal(sx, sy, xDiff)
                }
            } else {
                noneValue()
            }
        }
    }

    fun CenteredIntArray(size: Int) = CenteredIntArray(IntArray(size))

    @JvmInline
    value class CenteredIntArray(@JvmField val array: IntArray) {
        operator fun get(index: Int) = array[index + (array.size / 2)]
        operator fun set(index: Int, value: Int) {
            array[index + (array.size / 2)] = value
        }
    }

    fun <T> midPointFilteredArray(
        oldOrigin: Array<out T>, newOrigin: Array<out T>,
        cb: FilteredArrayDiffItemCallback<T>,
        oldStart: Int, oldEnd: Int, newStart: Int, newEnd: Int,
        forward: CenteredIntArray, backward: CenteredIntArray
    ): Snake? {
        val oldRangeSize = oldEnd - oldStart
        val newRangeSize = newEnd - newStart

        if (oldRangeSize < 1 || newRangeSize < 1) {
            return null
        }

        val max = (oldRangeSize + newRangeSize + 1) / 2

        forward[1] = oldStart
        backward[1] = oldEnd

        for (d in 0 until max) {
            forward(
                oldOrigin, newOrigin, cb, oldStart, oldEnd, newStart, newEnd, forward, backward, d
            )?.let {
                return it
            }

            backward(
                oldOrigin, newOrigin, cb, oldStart, oldEnd, newStart, newEnd, forward, backward, d
            )?.let {
                return it
            }
        }

        return null
    }

    private fun <T> forward(
        oldOrigin: Array<out T>, newOrigin: Array<out T>,
        cb: FilteredArrayDiffItemCallback<T>,
        oldStart: Int, oldEnd: Int, newStart: Int, newEnd: Int,
        forward: CenteredIntArray, backward: CenteredIntArray,
        d: Int
    ): Snake? {
        val delta = (oldEnd - oldStart) - (newEnd - newStart)
        val checkForSnake = abs(delta) % 2 == 1

        var k = -d
        while (k <= d) {
            // we either come from d-1, k-1 OR d-1. k+1
            // as we move in steps of 2, array always holds both current and previous d values
            // k = x - y and each array value holds the max X, y = x - k
            val startX: Int
            var x: Int

            val nextKItem = forward[k + 1]
            val prevKItem = forward[k - 1]

            if (k == -d || k != d && nextKItem > prevKItem) {
                // picking k + 1, incrementing Y (by simply not incrementing X)
                startX = nextKItem
                x = startX
            } else {
                // picking k - 1, incrementing X
                startX = prevKItem
                x = startX + 1
            }

            var y = newStart + (x - oldStart) - k
            val startY = if (d == 0 || x != startX) y else (y - 1)

            // now find snake size
            while (x < oldEnd && y < newEnd) {
                if (!cb.areItemsTheSame(oldOrigin[x], newOrigin[y])) {
                    break
                }

                x++
                y++
            }

            // now we have furthest reaching x, record it
            forward[k] = x
            if (checkForSnake) {
                // see if we did pass over a backwards array
                // mapping function: delta - k
                val backwardsK = delta - k

                // if backwards K is calculated and it passed me, found match
                if (backwardsK >= 1 - d && backwardsK <= d - 1 && backward[backwardsK] <= x) {
                    // match
                    return Snake(startX, startY, x, y, isReversed = false)
                }
            }

            k += 2
        }

        return null
    }

    private fun <T> backward(
        oldOrigin: Array<out T>, newOrigin: Array<out T>,
        cb: FilteredArrayDiffItemCallback<T>,
        oldStart: Int, oldEnd: Int, newStart: Int, newEnd: Int,
        forward: CenteredIntArray, backward: CenteredIntArray,
        d: Int
    ): Snake? {
        val delta = (oldEnd - oldStart) - (newEnd - newStart)
        val checkForSnake = abs(delta) % 2 == 0

        // same as forward but we go backwards from end of the lists to be beginning
        // this also means we'll try to optimize for minimizing x instead of maximizing it
        var k = -d
        while (k <= d) {
            // we either come from d-1, k-1 OR d-1, k+1
            // as we move in steps of 2, array always holds both current and previous d values
            // k = x - y and each array value holds the MIN X, y = x - k
            // when x's are equal, we prioritize deletion over insertion
            val startX: Int
            var x: Int

            val nextKItem = backward[k + 1]
            val prevKItem = backward[k - 1]

            if (k == -d || k != d && nextKItem < prevKItem) {
                // picking k + 1, decrementing Y (by simply not decrementing X)
                startX = nextKItem
                x = startX
            } else {
                // picking k - 1, decrementing X
                startX = prevKItem
                x = startX - 1
            }

            var y = newEnd - (oldEnd - x - k)
            val startY = if (d == 0 || x != startX) y else y + 1

            // now find snake size
            while (x > oldStart && y > newStart) {
                if (!cb.areItemsTheSame(oldOrigin[x - 1], newOrigin[y - 1])) {
                    break
                }

                x--
                y--
            }

            // now we have furthest point, record it (min X)
            backward[k] = x
            if (checkForSnake) {
                // see if we did pass over a backwards array
                // mapping function: delta - k
                val forwardsK = delta - k

                // if forwards K is calculated and it passed me, found match
                if (forwardsK >= -d && forwardsK <= d && forward[forwardsK] >= x) {
                    // assignment are reverse since we are a reverse snake
                    return Snake(x, y, startX, startY, isReversed = true)
                }
            }

            k += 2
        }

        return null
    }

    @Suppress("UNCHECKED_CAST")
    inline fun<TValue, TDiag, TDiagList> createDiffResult(
        oldArray: FilteredArray<out TValue>, newArray: FilteredArray<out TValue>,
        itemCallback: FilteredArrayDiffItemCallback<TValue>,
        statuses: IntArray,
        diagonals: TDiagList,
        crossinline diagListSize: TDiagList.() -> Int,
        crossinline diagListGet: TDiagList.(Int) -> TDiag,
        crossinline diagX: TDiag.() -> Int, crossinline diagY: TDiag.() -> Int, crossinline diagSize: TDiag.() -> Int,
        addEdgeDiagonals: () -> Unit,
    ): FilteredArrayDiffResult {
        Arrays.fill(statuses, 0)

        val oldOrigin = oldArray.origin
        val newOrigin = newArray.origin

        addEdgeDiagonals()

        for (i in 0 until diagonals.diagListSize()) {
            val diag = diagonals.diagListGet(i)
            val dx = diag.diagX()
            val dy = diag.diagY()

            for (offset in 0 until diag.diagSize()) {
                val posX = dx + offset
                val posY = dy + offset

                if (!itemCallback.areContentsTheSame(oldOrigin[posX], newOrigin[posY])) {
                    statuses[posX] = 1
                }
            }
        }

        return object : FilteredArrayDiffResult {
            override fun dispatchTo(callback: ListUpdateCallback) {
                var posX = oldArray.size
                var posY = newArray.size

                for (i in (diagonals.diagListSize() - 1) downTo 0) {
                    val diagonal = diagonals.diagListGet(i)
                    val dx = diagonal.diagX()
                    val dy = diagonal.diagY()
                    val dSize = diagonal.diagSize()

                    val endX = dx + dSize
                    val endY = dy + dSize

                    val removedCount = posX - endX
                    if (removedCount > 0) {
                        callback.onRemoved(endX, removedCount)
                    }

                    val insertedCount = posY - endY
                    if (insertedCount > 0) {
                        callback.onInserted(endX, insertedCount)
                    }

                    posX = dx
                    posY = dy

                    var lastChangePos = -1

                    for (j in 0 until dSize) {
                        val status = statuses[posX]

                        if (status == 1) {
                            if (lastChangePos < 0) {
                                lastChangePos = posX
                            }
                        } else {
                            // If the element isn't changed, it means the change range is ended,
                            // so invoke the callback and reset the range.
                            if (lastChangePos >= 0) {
                                callback.onChanged(lastChangePos, posX - lastChangePos)
                                lastChangePos = -1
                            }
                        }

                        posX++
                    }

                    // Process the last change range if it exists
                    if (lastChangePos >= 0) {
                        callback.onChanged(lastChangePos, posX - lastChangePos)
                    }

                    posX = dx
                }
            }
        }
    }
}