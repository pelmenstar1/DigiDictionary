package io.github.pelmenstar1.digiDict.common

/**
 * Represents a special wrapper of [Array] used to store elements after filtering.
 *
 * The elements that passed filtering are located starting from zero index up to [size].
 */
class FilteredArray<T>(
    val origin: Array<T>,
    override val size: Int
) : SizedIterable<T> {
    operator fun get(index: Int): T {
        // The important thing is to not let reading from valid index in context of origin but invalid index in context of size
        // If the index is negative, the exception will be thrown when accessing the origin.
        if (index >= size) {
            throw IndexOutOfBoundsException("index")
        }

        return origin[index]
    }

    override fun equals(other: Any?) = equalsPattern(other) { o ->
        val size = size

        if (size != o.size) return false

        val thisOrigin = origin
        val otherOrigin = o.origin

        for (i in 0 until size) {
            if (thisOrigin[i] != otherOrigin[i]) {
                return false
            }
        }

        return true
    }

    override fun hashCode(): Int {
        val size = size
        val origin = origin

        var result = size

        for (i in 0 until size) {
            result = result * 31 + origin[i].hashCode()
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

            for (i in 0 until size) {
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
        private val EMPTY = FilteredArray<Any>(emptyArray(), size = 0)

        @Suppress("UNCHECKED_CAST")
        fun <T> empty() = EMPTY as FilteredArray<T>
    }
}

/**
 * Filters given array using [predicate] lambda to determine whether an element [T] passed filtering or not.
 *
 * **Note that the given array is mutated**
 */
inline fun <T> Array<T>.toFilteredArray(predicate: (element: T) -> Boolean): FilteredArray<T> {
    var currentIndex = 0

    for (i in indices) {
        val element = this[i]

        if (predicate(element)) {
            val t = this[currentIndex]
            this[currentIndex] = element
            this[i] = t

            currentIndex++
        }
    }

    return if (currentIndex > 0) {
        FilteredArray(this, currentIndex)
    } else {
        FilteredArray.empty()
    }
}