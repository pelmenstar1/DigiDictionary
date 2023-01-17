package io.github.pelmenstar1.digiDict.common

internal object ArrayFilterDiffShort {
    fun PackedRange(oldStart: Int, oldEnd: Int, newStart: Int, newEnd: Int): PackedRange {
        val bits = ((oldStart.toLong() and 0xFFFF) shl PackedRange.OLD_START_SHIFT) or
                ((oldEnd.toLong() and 0xFFFF) shl PackedRange.OLD_END_SHIFT) or
                ((newStart.toLong() and 0xFFFF) shl PackedRange.NEW_START_SHIFT) or
                (newEnd.toLong() and 0xFFFF)

        return PackedRange(bits)
    }

    @JvmInline
    value class PackedRange(@JvmField val bits: Long) {
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

            internal val NONE = PackedRange(-1L)
        }
    }

    fun PackedDiagonal(x: Int, y: Int, size: Int): PackedDiagonal {
        val bits = ((x and 0xFFFF).toLong() shl PackedDiagonal.X_SHIFT) or
                ((y and 0xFFFF).toLong() shl PackedDiagonal.Y_SHIFT) or
                (size and 0xFFFF).toLong()

        return PackedDiagonal(bits)
    }

    @JvmInline
    value class PackedDiagonal(@JvmField val bits: Long) {
        val x: Int
            get() = (bits shr X_SHIFT).toInt() and 0xFFFF

        val y: Int
            get() = (bits shr Y_SHIFT).toInt() and 0xFFFF

        val size: Int
            get() = bits.toInt() and 0xFFFF

        companion object {
            internal const val X_SHIFT = 32
            internal const val Y_SHIFT = 16

            val NONE = PackedDiagonal(-1L)
        }
    }

    class PackedDiagonalList {
        @JvmField
        var elements: LongArray = EmptyArray.LONG

        @JvmField
        var size = 0

        operator fun get(index: Int) = PackedDiagonal(elements[index])

        fun addLast(value: PackedDiagonal) {
            elements = LongListHelper.addLast(elements, size, value.bits)
            size++
        }

        fun addFirst(value: PackedDiagonal) {
            elements = LongListHelper.addFirst(elements, size, value.bits)
            size++
        }

        // Sorts the elements as they were sorted using DIAGONAL_COMPARATOR
        fun sort() {
            HeapSort.sort(
                elements, size,
                LongArray::get, LongArray::set,
                compare = { a, b -> PackedDiagonal(a).x - PackedDiagonal(b).x }
            )
        }
    }

    private class PackedRangeStack {
        private var elements: LongArray = EmptyArray.LONG

        @JvmField
        var size = 0

        operator fun get(index: Int) = PackedDiagonal(elements[index])

        fun push(value: PackedRange) {
            elements = LongListHelper.addLast(elements, size, value.bits)
            size++
        }

        fun pop(): PackedRange {
            var currentSize = size
            if (currentSize == 0) {
                return PackedRange.NONE
            }

            val lastElement = elements[--currentSize]
            size = currentSize

            return PackedRange(lastElement)
        }
    }

    fun <T> calculateDifference(
        oldArray: FilteredArray<out T>,
        newArray: FilteredArray<out T>,
        cb: FilteredArrayDiffItemCallback<T>
    ): FilteredArrayDiffResult {
        val oldSize = oldArray.size
        val newSize = newArray.size

        val oldOrigin = oldArray.origin
        val newOrigin = newArray.origin

        val diagonals = PackedDiagonalList()

        // instead of a recursive implementation, we keep our own stack to avoid potential stack
        // overflow exceptions
        val stack = PackedRangeStack()
        stack.push(PackedRange(0, oldSize, 0, newSize))

        val max = (oldSize + newSize + 1) / 2

        // allocate forward and backward k-lines. K lines are diagonal lines in the matrix. (see the
        // paper for details)
        // These arrays lines keep the max reachable position for each k-line.
        val forward = CenteredIntArray(max * 2 + 1)
        val backward = CenteredIntArray(max * 2 + 1)

        while (stack.size > 0) {
            val range = stack.pop()

            val snake = ArrayFilterDiffShared.midPointFilteredArray(
                oldOrigin, newOrigin,
                cb,
                range.oldStart, range.oldEnd, range.newStart, range.newEnd,
                forward, backward
            )

            if (snake != null) {
                // if it has a diagonal, save it
                snake.toPackedDiagonal().let {
                    if (it != PackedDiagonal.NONE) {
                        diagonals.addLast(it)
                    }
                }

                stack.push(PackedRange(range.oldStart, snake.startX, range.newStart, snake.startY))
                stack.push(PackedRange(snake.endX, range.oldEnd, snake.endY, range.newEnd))
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
        diagonals: PackedDiagonalList,
    ): FilteredArrayDiffResult {
        return ArrayFilterDiffShared.createDiffResult(
            oldArray, newArray, cb, statuses, diagonals,
            PackedDiagonalList::size, PackedDiagonalList::get,
            PackedDiagonal::x, PackedDiagonal::y, PackedDiagonal::size,
            addEdgeDiagonals = {
                val first = if (diagonals.size > 0) diagonals[0] else PackedDiagonal.NONE

                if (first == PackedDiagonal.NONE || first.x != 0 || first.y != 0) {
                    diagonals.addFirst(PackedDiagonal(0))
                }

                diagonals.addLast(PackedDiagonal(oldArray.size, newArray.size, 0))
            }
        )
    }
}