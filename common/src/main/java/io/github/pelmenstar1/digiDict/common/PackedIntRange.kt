@file:Suppress("NOTHING_TO_INLINE")

package io.github.pelmenstar1.digiDict.common

/**
 * Creates an instance of [PackedIntRange].
 * [start] should not be greater than [end], otherwise the [IllegalArgumentException] will be thrown.
 */
fun PackedIntRange(start: Int, end: Int): PackedIntRange {
    if (start > end) {
        throw IllegalArgumentException("start > end")
    }

    val bits = (start.toLong() shl 32) or (end.toLong() and 0xFFFFFFFF)

    return PackedIntRange(bits)
}

/**
 * Represents a half-open [Int] range where [start] is inclusive and [end] is exclusive.
 *
 * It's a value class and the start and the end of the range is saved in [Long].
 * If the range needs to be saved in array, don't use `Array<PackedIntRange>` because
 * it will box all the instances of [PackedIntRange]. Instead, use [PackedIntRangeArray], it resolves such a problem.
 *
 * The class supports zero-length ranges when start and end are equal
 */
@JvmInline
value class PackedIntRange(@JvmField val bits: Long) {
    /**
     * Start (inclusive) value of the range.
     */
    val start: Int
        get() = (bits ushr 32).toInt()

    /**
     * End (exclusive) value of the range.
     */
    val end: Int
        get() = (bits and 0xFFFFFFFF).toInt()

    inline operator fun component1() = start
    inline operator fun component2() = end

    override fun toString(): String {
        return "PackedIntRange(start=$start, end=$end)"
    }
}

/**
 * Wraps the [LongArray] and provides the methods to operate the array as it were the array of [PackedIntRange].
 * It resolves the problem of boxing value classes in ordinary object arrays.
 */
@JvmInline
value class PackedIntRangeArray(@JvmField val rawArray: LongArray) {
    /**
     * Size of the array.
     */
    inline val size: Int
        get() = rawArray.size

    /**
     * Returns an element at given [index].
     */
    inline operator fun get(index: Int): PackedIntRange {
        return PackedIntRange(rawArray[index])
    }

    /**
     * Sets given [value] to specified [index].
     */
    inline operator fun set(index: Int, value: PackedIntRange) {
        rawArray[index] = value.bits
    }

    /**
     * Applies [transform] lambda to each element of the [PackedIntRangeArray]
     * and saves transformed elements to [Array].
     */
    inline fun <reified T> map(transform: (PackedIntRange) -> T): Array<T> {
        val arr = rawArray

        return Array(arr.size) { i ->
            transform(PackedIntRange(arr[i]))
        }
    }
}

/**
 * Represents a list of [PackedIntRange].
 * It resolves the problem of boxing value classes in ordinary [List]'s.
 */
class PackedIntRangeList(capacity: Int = 0) {
    private var elements = LongArray(capacity)

    /**
     * Current size of the list
     */
    var size: Int = 0
        private set

    /**
     * Adds specified [element] to the list.
     * If the list has sufficient capacity, it won't allocate.
     */
    fun add(element: PackedIntRange) {
        elements = LongListHelper.addLast(elements, size, element.bits)
        size++
    }

    /**
     * Converts the list to [PackedIntRangeArray].
     * It's unsafe because if the list capacity and size are the same,
     * it returns an underlying array of [PackedIntRangeList] that might be mutated outside of this class.
     */
    fun toArrayUnsafe(): PackedIntRangeArray {
        val elements = elements

        val resultElements = if (elements.size == size) {
            elements
        } else {
            elements.copyOf(size)
        }

        return PackedIntRangeArray(resultElements)
    }
}