package io.github.pelmenstar1.digiDict.common

import java.util.*
import kotlin.math.abs
import kotlin.math.min

private class Range {
    @JvmField
    var oldStart = 0

    @JvmField
    var oldEnd = 0

    @JvmField
    var newStart = 0

    @JvmField
    var newEnd = 0

    val oldSize: Int
        get() = oldEnd - oldStart

    val newSize: Int
        get() = newEnd - newStart

    constructor()
    constructor(oldStart: Int, oldEnd: Int, newStart: Int, newEnd: Int) {
        this.oldStart = oldStart
        this.oldEnd = oldEnd
        this.newStart = newStart
        this.newEnd = newEnd
    }
}

class Diagonal(@JvmField val x: Int, @JvmField val y: Int, @JvmField val size: Int) {
    val endX: Int
        get() = x + size

    val endY: Int
        get() = y + size
}

private class Snake(
    @JvmField var startX: Int,
    @JvmField var startY: Int,
    @JvmField var endX: Int,
    @JvmField var endY: Int,
    @JvmField var isReversed: Boolean
) {
    fun toDiagonal(): Diagonal? {
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
                    Diagonal(sx, sy, dSize)
                } else {
                    // snake edge it at the beginning
                    if (yDiff > xDiff) {
                        Diagonal(sx, sy + 1, dSize)
                    } else {
                        Diagonal(sx + 1, sy, dSize)
                    }
                }
            } else {
                // we are a pure diagonal
                Diagonal(sx, sy, xDiff)
            }
        } else {
            null
        }
    }
}

class FilteredArrayDiffResult<T> internal constructor(
    private val diagonals: ArrayList<Diagonal>,
    private val oldArray: FilteredArray<T>,
    private val newArray: FilteredArray<T>,
    private val oldStatuses: IntArray,
    private val newStatuses: IntArray,
    private val itemCallback: FilteredArrayDiffItemCallback<T>
) {
    init {
        Arrays.fill(oldStatuses, 0)
        Arrays.fill(newStatuses, 0)

        addEdgeDiagonals()
        findMatchingItems()
    }

    private fun addEdgeDiagonals() {
        val first = diagonals.firstOrNull()

        if(first == null || first.x != 0 || first.y != 0) {
            diagonals.add(0, Diagonal(0, 0, 0))
        }

        diagonals.add(Diagonal(oldArray.size, newArray.size, 0))
    }

    private fun findMatchingItems() {
        val oldOrigin = oldArray.origin
        val oldBitSet = oldArray.bitSet
        val oldMap = oldArray.postBitSetMap

        val newOrigin = newArray.origin
        val newBitSet = newArray.bitSet
        val newMap = newArray.postBitSetMap

        for (diagonal in diagonals) {
            val diagX = diagonal.x
            val diagY = diagonal.y

            var xBitPos = oldBitSet.findPositionOfNthSetBit(diagX)
            var yBitPos = newBitSet.findPositionOfNthSetBit(diagY)

            for (offset in 0 until diagonal.size) {
                val oldIndex = mapIndex(xBitPos, oldMap)
                val newIndex = mapIndex(yBitPos, newMap)

                if(!itemCallback.areContentsTheSame(oldOrigin[oldIndex], newOrigin[newIndex])) {
                    val posX = diagX + offset
                    val posY = diagY + offset

                    oldStatuses[posX] = STATUS_CHANGED
                    newStatuses[posY] = STATUS_CHANGED
                }

                xBitPos = oldBitSet.nextSetBit(fromIndex = xBitPos + 1)
                yBitPos = newBitSet.nextSetBit(fromIndex = yBitPos + 1)
            }
        }
    }

    fun dispatchTo(callback: ListUpdateCallback) {
        var posX = oldArray.size
        var posY = newArray.size

        for(i in (diagonals.size - 1) downTo 0) {
            val diagonal = diagonals[i]
            val diagX = diagonal.x
            val diagY = diagonal.y

            val endX = diagonal.endX
            val endY = diagonal.endY

            val removedCount = posX - endX
            if(removedCount > 0) {
                callback.onRemoved(endX, removedCount)
            }

            val insertedCount = posY - endY
            if(insertedCount > 0) {
                callback.onInserted(endX, insertedCount)
            }

            posX = diagX
            posY = diagY

            var lastChangePos = -1

            for(j in 0 until diagonal.size) {
                val status = oldStatuses[posX]

                if (status == STATUS_CHANGED) {
                    if(lastChangePos < 0) {
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

            posX = diagX
        }
    }

    companion object {
        private const val STATUS_CHANGED = 1
        private const val STATUS_NOT_CHANGED = 0
    }
}

interface FilteredArrayDiffItemCallback<in T> {
    fun areItemsTheSame(a: T, b: T): Boolean
    fun areContentsTheSame(a: T, b: T): Boolean
}

private val DIAGONAL_COMPARATOR = Comparator<Diagonal> { a, b -> a.x - b.x }

fun <T> FilteredArray<T>.calculateDifference(
    other: FilteredArray<T>,
    cb: FilteredArrayDiffItemCallback<T>
): FilteredArrayDiffResult<T> {
    val oldArray = this

    @Suppress("UnnecessaryVariable")
    val newArray = other

    val oldSize = oldArray.size
    val newSize = newArray.size

    val diagonals = ArrayList<Diagonal>()

    // instead of a recursive implementation, we keep our own stack to avoid potential stack
    // overflow exceptions
    val stack = ArrayList<Range>()
    stack.add(Range(0, oldSize, 0, newSize))

    val max = (oldSize + newSize + 1) / 2

    // allocate forward and backward k-lines. K lines are diagonal lines in the matrix. (see the
    // paper for details)
    // These arrays lines keep the max reachable position for each k-line.
    val forward = IntArray(max * 2 + 1)
    val backward = IntArray(max * 2 + 1)

    // We pool the ranges to avoid allocations for each recursive call.
    val rangePool = ArrayList<Range>()
    while (stack.isNotEmpty()) {
        val range = stack.removeAt(stack.size - 1)
        val snake = midPointFilteredArray(oldArray, newArray, cb, range, forward, backward)

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

    return FilteredArrayDiffResult(diagonals, oldArray, newArray, forward, backward, cb)
}

private fun <T> midPointFilteredArray(
    oldArray: FilteredArray<T>,
    newArray: FilteredArray<T>,
    cb: FilteredArrayDiffItemCallback<T>,
    range: Range,
    forward: IntArray,
    backward: IntArray
): Snake? {
    val oldRangeSize = range.oldSize
    val newRangeSize = range.newSize

    if (oldRangeSize < 1 || newRangeSize < 1) {
        return null
    }

    val max = (oldRangeSize + newRangeSize + 1) / 2

    forward.setCentered(1, range.oldStart)
    backward.setCentered(1, range.oldEnd)

    for (d in 0 until max) {
        forwardFilteredArray(oldArray, newArray, cb, range, forward, backward, d)?.let {
            return it
        }

        backwardFilteredArray(oldArray, newArray, cb, range, forward, backward, d)?.let {
            return it
        }
    }

    return null
}

private fun <T> forwardFilteredArray(
    oldArray: FilteredArray<T>,
    newArray: FilteredArray<T>,
    cb: FilteredArrayDiffItemCallback<T>,
    range: Range,
    forward: IntArray,
    backward: IntArray,
    d: Int
): Snake? {
    val oldStart = range.oldStart
    val oldEnd = range.oldEnd
    val newStart = range.newStart
    val newEnd = range.newEnd

    val delta = (oldEnd - oldStart) - (newEnd - newStart)
    val checkForSnake = abs(delta) % 2 == 1

    val oldOrigin = oldArray.origin
    val newOrigin = newArray.origin

    val oldBitSet = oldArray.bitSet
    val newBitSet = newArray.bitSet

    val oldMap = oldArray.postBitSetMap
    val newMap = newArray.postBitSetMap

    var k = -d
    while (k <= d) {
        // we either come from d-1, k-1 OR d-1. k+1
        // as we move in steps of 2, array always holds both current and previous d values
        // k = x - y and each array value holds the max X, y = x - k
        val startX: Int
        var x: Int

        val nextKItem = forward.getCentered(k + 1)
        val prevKItem = forward.getCentered(k - 1)

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

        var xBitPos = oldBitSet.findPositionOfNthSetBit(x)
        var yBitPos = newBitSet.findPositionOfNthSetBit(y)

        // now find snake size
        while (x < oldEnd && y < newEnd) {
            val oldIndex = mapIndex(xBitPos, oldMap)
            val newIndex = mapIndex(yBitPos, newMap)

            if (!cb.areItemsTheSame(oldOrigin[oldIndex], newOrigin[newIndex])) {
                break
            }

            xBitPos = oldBitSet.nextSetBit(fromIndex = xBitPos + 1)
            yBitPos = newBitSet.nextSetBit(fromIndex = yBitPos + 1)

            x++
            y++
        }

        // now we have furthest reaching x, record it
        forward.setCentered(k, x)
        if (checkForSnake) {
            // see if we did pass over a backwards array
            // mapping function: delta - k
            val backwardsK = delta - k

            // if backwards K is calculated and it passed me, found match
            if (backwardsK >= 1 - d && backwardsK <= d - 1 && backward.getCentered(backwardsK) <= x) {
                // match
                return Snake(startX, startY, x, y, isReversed = false)
            }
        }

        k += 2
    }

    return null
}

private fun <T> backwardFilteredArray(
    oldArray: FilteredArray<T>,
    newArray: FilteredArray<T>,
    cb: FilteredArrayDiffItemCallback<T>,
    range: Range,
    forward: IntArray,
    backward: IntArray,
    d: Int
): Snake? {
    val oldStart = range.oldStart
    val oldEnd = range.oldEnd
    val newStart = range.newStart
    val newEnd = range.newEnd

    val delta = (oldEnd - oldStart) - (newEnd - newStart)
    val checkForSnake = abs(delta) % 2 == 0

    val oldOrigin = oldArray.origin
    val newOrigin = newArray.origin

    val oldBitSet = oldArray.bitSet
    val newBitSet = newArray.bitSet

    val oldMap = oldArray.postBitSetMap
    val newMap = newArray.postBitSetMap

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

        val nextKItem = backward.getCentered(k + 1)
        val prevKItem = backward.getCentered(k - 1)

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

        var xBitPos = oldBitSet.findPositionOfNthSetBit(x - 1)
        var yBitPos = newBitSet.findPositionOfNthSetBit(y - 1)

        // now find snake size
        while(x > oldStart && y > newStart) {
            val oldIndex = mapIndex(xBitPos, oldMap)
            val newIndex = mapIndex(yBitPos, newMap)

            if (!cb.areItemsTheSame(oldOrigin[oldIndex], newOrigin[newIndex])) {
                break
            }

            x--
            y--

            xBitPos = oldBitSet.previousSetBit(fromIndex = xBitPos - 1)
            yBitPos = newBitSet.previousSetBit(fromIndex = yBitPos - 1)
        }

        // now we have furthest point, record it (min X)
        backward.setCentered(k, x)
        if (checkForSnake) {
            // see if we did pass over a backwards array
            // mapping function: delta - k
            val forwardsK = delta - k

            // if forwards K is calculated and it passed me, found match
            if (forwardsK >= -d && forwardsK <= d && forward.getCentered(forwardsK) >= x) {
                // assignment are reverse since we are a reverse snake
                return Snake(x, y, startX, startY, isReversed = true)
            }
        }

        k += 2
    }

    return null
}

private fun IntArray.getCentered(index: Int): Int {
    return this[index + (size / 2)]
}

private fun IntArray.setCentered(index: Int, value: Int) {
    this[index + (size / 2)] = value
}

internal fun mapIndex(index: Int, map: IntArray?): Int {
    return if (map != null) map[index] else index
}