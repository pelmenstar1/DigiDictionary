package io.github.pelmenstar1.digiDict.common

import kotlin.math.min

class FilteredArray<out T>(
    private val origin: Array<out T>,

    // If the bit at position N is set, it means that element N in origin passed a filtering.
    private val bitSet: LongArray,

    // If 'size' argument is negative, size will be computed using bitSet and Long.countOneBits
    //
    // If 'size' argument is positive or zero, it will be used to init size. It allows to skip countOneBits part.
    // 'size' argument must equal to the count of set bits in bitSet, otherwise it may lead to unexpected results.
    size: Int = -1
) : SizedIterable<T> {
    override val size: Int = if (size >= 0) size else bitSet.sumOf(Long::countOneBits)

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

        val origin = origin
        val bitSet = bitSet

        val otherOrigin = other.origin
        val otherBitSet = other.bitSet

        if (origin === otherOrigin) {
            return bitSet.contentEquals(otherBitSet)
        }

        var result = true

        var otherBitIndex = otherBitSet.nextSetBit(0)

        bitSet.iterateSetBits { absIndex ->
            if (origin[absIndex] != otherOrigin[otherBitIndex]) {
                result = false
                return@iterateSetBits
            }

            otherBitIndex = otherBitSet.nextSetBit(otherBitIndex + 1)
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
            val origin = origin
            val size = size

            append("FilteredArray(size=")
            append(size)
            append(", elements=[")

            var seqIndex = 0
            bitSet.iterateSetBits { i ->
                append(origin[i])

                if ((seqIndex++) < size - 1) {
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

inline fun <E> Array<E>.filterFast(predicate: (element: E) -> Boolean): FilteredArray<E> {
    val size = size

    // Ceiling division to 64
    val bitSetSize = (size + 63) shr 6
    val bitSet = LongArray(bitSetSize)

    var wordIndex = 0
    var start = 0
    var filteredSize = 0

    // Fill bitSet word by word
    while (start < size) {
        // end shouldn't be greater than size of the array
        val end = min(start + 64, size)

        var word = 0L

        for (i in start until end) {
            if (predicate(this[i])) {
                filteredSize++

                // Two facts make (1L shl i) mask valid:
                // - left shift operator takes into account only 6 lowest-order bits
                //   which means that (1L shl i) is actually (1L shl (i % 64))
                // - start is always aligned to 64, which means that (1L shl start) is (1L shl 0),
                //   (1L shl (start + 1)) is equals to (1L shl 1) and so on.
                //   To generalize, say we have variable n within [start; end) range and (end - start) <= 64, then:
                //   (1L shl n) = (1L shl (n - start))
                word = word or (1L shl i)
            }
        }

        bitSet[wordIndex++] = word

        start = end
    }

    return FilteredArray(this, bitSet, size = filteredSize)
}