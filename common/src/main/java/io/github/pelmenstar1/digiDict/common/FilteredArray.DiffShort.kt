package io.github.pelmenstar1.digiDict.common

internal fun PackedDiffRange(oldStart: Int, oldEnd: Int, newStart: Int, newEnd: Int): PackedDiffRange {
    val bits = ((oldStart.toLong() and 0xFFFF) shl PackedDiffRange.OLD_START_SHIFT) or
            ((oldEnd.toLong() and 0xFFFF) shl PackedDiffRange.OLD_END_SHIFT) or
            ((newStart.toLong() and 0xFFFF) shl PackedDiffRange.NEW_START_SHIFT) or
            (newEnd.toLong() and 0xFFFF)

    return PackedDiffRange(bits)
}

@JvmInline
internal value class PackedDiffRange(@JvmField val bits: Long) {
    val oldStart: Int
        get() = (bits ushr OLD_START_SHIFT).toInt()

    val oldEnd: Int
        get() = (bits shr OLD_END_SHIFT).toInt() and 0xFFFF

    val newStart: Int
        get() = (bits shr NEW_START_SHIFT).toInt() and 0xFFFF

    val newEnd: Int
        get() = bits.toInt() and 0xFFFF

    companion object {
        internal const val OLD_START_SHIFT = 48
        internal const val OLD_END_SHIFT = 32
        internal const val NEW_START_SHIFT = 16
    }
}

internal fun PackedDiffDiagonal(x: Int, y: Int, size: Int): PackedDiffDiagonal {
    val bits = ((x and 0xFFFF).toLong() shl PackedDiffDiagonal.X_SHIFT) or
            ((y and 0xFFFF).toLong() shl PackedDiffDiagonal.Y_SHIFT) or
            (size and 0xFFFF).toLong()

    return PackedDiffDiagonal(bits)
}

@JvmInline
internal value class PackedDiffDiagonal(@JvmField val bits: Long) {
    val x: Int
        get() = (bits shr X_SHIFT).toInt() and 0xFFFF

    val y: Int
        get() = (bits shr Y_SHIFT).toInt() and 0xFFFF

    val size: Int
        get() = bits.toInt() and 0xFFFF

    companion object {
        internal const val X_SHIFT = 32
        internal const val Y_SHIFT = 16

        val NONE = PackedDiffDiagonal(-1L)
    }
}

internal class PackedDiffDiagonalList {
    @JvmField
    var elements: LongArray = EmptyArray.LONG

    @JvmField
    var size = 0

    operator fun get(index: Int) = PackedDiffDiagonal(elements[index])

    fun addLast(value: PackedDiffDiagonal) {
        elements = LongListHelper.addLast(elements, size, value.bits)
        size++
    }

    fun addFirst(value: PackedDiffDiagonal) {
        elements = LongListHelper.addFirst(elements, size, value.bits)
        size++
    }

    // Sorts the elements as they were sorted using DIAGONAL_COMPARATOR
    fun sort() {
        HeapSort.sort(
            elements, size,
            LongArray::get, LongArray::set,
            compare = { a, b -> PackedDiffDiagonal(a).x - PackedDiffDiagonal(b).x }
        )
    }
}

internal class PackedDiffRangeStack {
    private var elements: LongArray = EmptyArray.LONG

    @JvmField
    var size = 0

    operator fun get(index: Int) = PackedDiffDiagonal(elements[index])

    fun push(value: PackedDiffRange) {
        elements = LongListHelper.addLast(elements, size, value.bits)
        size++
    }

    fun pop(): PackedDiffRange {
        val lastElement = elements[--size]

        return PackedDiffRange(lastElement)
    }
}

internal class FilteredArrayDiffManagerDelegateShortImpl<T> : FilteredArrayDiffManagerDelegate<T>() {
    private val diagonals = PackedDiffDiagonalList()

    override fun calculateDifference(
        oldArray: FilteredArray<out T>,
        newArray: FilteredArray<out T>,
        cb: FilteredArrayDiffItemCallback<T>
    ): FilteredArrayDiffResult {
        val oldSize = oldArray.size
        val newSize = newArray.size

        val oldOrigin = oldArray.origin
        val newOrigin = newArray.origin

        // Behave as if the list is empty but it won't allocate if there's space for a diagonal
        diagonals.size = 0

        // instead of a recursive implementation, we keep our own stack to avoid potential stack
        // overflow exceptions
        val stack = PackedDiffRangeStack()
        stack.push(PackedDiffRange(0, oldSize, 0, newSize))

        initForwardBackwardArrays(max = (oldSize + newSize + 1) / 2)

        val forward = forwardArray
        val backward = backwardArray

        while (stack.size > 0) {
            val range = stack.pop()

            val snake = FilteredArrayDiffShared.midPointFilteredArray(
                oldOrigin, newOrigin,
                cb,
                range.oldStart, range.oldEnd, range.newStart, range.newEnd,
                forward, backward
            )

            if (snake != null) {
                // if it has a diagonal, save it
                snake.toPackedDiagonal().let {
                    if (it != PackedDiffDiagonal.NONE) {
                        diagonals.addLast(it)
                    }
                }

                stack.push(PackedDiffRange(range.oldStart, snake.startX, range.newStart, snake.startY))
                stack.push(PackedDiffRange(snake.endX, range.oldEnd, snake.endY, range.newEnd))
            }
        }

        // sort snakes
        diagonals.sort()

        return createDiffResult(oldArray, newArray, cb, forward.array, diagonals)
    }

    private fun <T> createDiffResult(
        oldArray: FilteredArray<out T>,
        newArray: FilteredArray<out T>,
        cb: FilteredArrayDiffItemCallback<T>,
        statuses: IntArray,
        diagonals: PackedDiffDiagonalList,
    ): FilteredArrayDiffResult {
        return FilteredArrayDiffShared.createDiffResult(
            oldArray, newArray, cb, statuses, diagonals,
            PackedDiffDiagonalList::size, PackedDiffDiagonalList::get,
            PackedDiffDiagonal::x, PackedDiffDiagonal::y, PackedDiffDiagonal::size,
            addEdgeDiagonals = {
                val first = if (diagonals.size > 0) diagonals[0] else PackedDiffDiagonal.NONE

                if (first == PackedDiffDiagonal.NONE || first.x != 0 || first.y != 0) {
                    diagonals.addFirst(PackedDiffDiagonal(0))
                }

                diagonals.addLast(PackedDiffDiagonal(oldArray.size, newArray.size, 0))
            }
        )
    }
}