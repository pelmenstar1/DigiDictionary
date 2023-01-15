package io.github.pelmenstar1.digiDict.common

/**
 * Returns new [FilteredArray] that represents sorted given [FilteredArray] using specified [comparator] to compare values.
 *
 * It's using Heap Sort algorithm which means the sorting is unstable.
 *
 * **Note that the underlying array of given [FilteredArray] is mutated**
 */
fun <T> FilteredArray<T>.sorted(comparator: Comparator<in T>): FilteredArray<T> {
    val size = size

    if (size == 0) {
        return FilteredArray.empty()
    }

    val origin = origin

    buildMaxHeap(origin, size, comparator)

    var i = size - 1
    while (i > 0) {
        // Swap value of first indexed with last indexed
        origin.swap(0, i)

        // Maintaining heap property after each swapping
        var j = 0
        var index: Int

        do {
            index = 2 * j + 1

            if (index < i) {
                var indexValue = origin[index]
                val nextIndexValue = origin[index + 1]

                if (index < i - 1 && comparator.compare(indexValue, nextIndexValue) < 0) {
                    index++
                    indexValue = nextIndexValue
                }

                val jValue = origin[j]
                if (comparator.compare(jValue, indexValue) < 0) {
                    // Swap the values
                    origin[j] = indexValue
                    origin[index] = jValue
                }
            }

            j = index
        } while (index < i)

        i--
    }

    return FilteredArray(origin, size)
}

// Builds Max Heap where value of each child is always smaller than value of their parent.
private fun <T> buildMaxHeap(origin: Array<T>, size: Int, comparator: Comparator<in T>) {
    for (i in 1 until size) {
        // If child is bigger than parent
        if (comparator.compare(origin[i], origin[(i - 1) / 2]) > 0) {
            var j = i

            // Swap child and parent until parent is smaller
            while (true) {
                val parentIndex = (j - 1) / 2

                val child = origin[j]
                val parent = origin[parentIndex]

                if (comparator.compare(child, parent) <= 0) {
                    break
                }

                // Swap child and parent
                origin[j] = parent
                origin[parentIndex] = child

                j = parentIndex
            }
        }
    }
}