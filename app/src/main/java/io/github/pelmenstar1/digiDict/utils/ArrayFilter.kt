package io.github.pelmenstar1.digiDict.utils

import io.github.pelmenstar1.digiDict.EmptyArray
import kotlin.math.min

class FilteredArray<T>(
    private val origin: Array<out T>,

    // If the bit at position N is set, it means that element N in origin passed a filtering.
    private val bitSet: LongArray
) : Collection<T> {
    override val size: Int = bitSet.sumOf(Long::countOneBits)

    override fun isEmpty() = size == 0

    override fun contains(element: T): Boolean {
        var result = false

        bitSet.iterateSetBits { i ->
            if (origin[i] == element) {
                result = true
                return@iterateSetBits
            }
        }

        return result
    }

    override fun containsAll(elements: Collection<T>) = elements.all { contains(it) }

    operator fun get(index: Int): T {
        return origin[resolveIndex(index)]
    }

    // Finds such a position N, that range [0; N] of bitSet has 'index' set bits.
    private fun resolveIndex(index: Int): Int {
        return bitSet.findPositionOfNthSetBit(index)
    }

    override fun equals(other: Any?): Boolean {
        if (other === this) return true
        if (other == null || javaClass != other.javaClass) return false

        other as FilteredArray<*>

        if (size != other.size) return false

        if (origin.contentEquals(other.origin)) {
            return bitSet.contentEquals(other.bitSet)
        }

        var seqIndex = 0
        var result = true

        bitSet.iterateSetBits { absIndex ->
            if (origin[absIndex] != other[seqIndex]) {
                result = false
                return@iterateSetBits
            }

            seqIndex++
        }

        return result
    }

    override fun hashCode(): Int {
        var result = 0

        bitSet.iterateSetBits { index ->
            result = result * 31 + origin[index].hashCode()
        }

        return result
    }

    override fun toString(): String {
        return buildString {
            append("FilteredArray(size=")
            append(size)
            append(", elements=[")

            bitSet.iterateSetBits { i ->
                append(origin[i])

                if (i < size - 1) {
                    append(", ")
                }
            }
            append("])")
        }
    }

    override fun iterator(): Iterator<T> {
        return object : Iterator<T> {
            private var index = 0

            override fun hasNext() = index < size
            override fun next() = get(index++)
        }
    }

    companion object {
        private val EMPTY = FilteredArray<Any>(emptyArray(), EmptyArray.LONG)

        @Suppress("UNCHECKED_CAST")
        fun <T> empty() = EMPTY as FilteredArray<T>
    }
}

inline fun <E> Array<out E>.filterToBitSet(predicate: (element: E) -> Boolean): LongArray {
    val size = size

    // Ceiling division to 64
    val bitSetSize = (size + 63) ushr 6
    val bitSet = LongArray(bitSetSize)

    var wordIndex = 0
    var start = 0

    // Fill bitset word by word
    while (start < size) {
        // end shouldn't overlap the array size, so limit it to size.
        val end = min(start + 64, size)

        var word = 0L

        for (i in start until end) {
            if (predicate(this[i])) {
                word = word or (1L shl i)
            }
        }

        bitSet[wordIndex++] = word

        start = end
    }

    return bitSet
}