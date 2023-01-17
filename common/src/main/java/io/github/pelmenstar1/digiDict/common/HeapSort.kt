package io.github.pelmenstar1.digiDict.common

/**
 * Contains some util methods that generalize Heap Sort algorithm on any type of array.
 */
object HeapSort {
    /**
     * Sorts the given [elements] in-place using Heap Sort algorithm
     */
    inline fun <TArray, TElement> sort(
        elements: TArray,
        size: Int,
        get: TArray.(Int) -> TElement,
        set: TArray.(Int, TElement) -> Unit,
        compare: (TElement, TElement) -> Int
    ) {
        // Build Max Heap where value of each child is always smaller than value of their parent.
        for (i in 1 until size) {
            // If child is bigger than parent
            if (compare(elements.get(i), elements.get((i - 1) / 2)) > 0) {
                var j = i

                // Swap child and parent until parent is smaller
                while (true) {
                    val parentIndex = (j - 1) / 2

                    val child = elements.get(j)
                    val parent = elements.get(parentIndex)

                    if (compare(child, parent) <= 0) {
                        break
                    }

                    // Swap child and parent
                    elements.set(j, parent)
                    elements.set(parentIndex, child)

                    j = parentIndex
                }
            }
        }

        var i = size - 1
        while (i > 0) {
            // Swap value of first indexed with last indexed
            elements.swap(0, i, get, set)

            // Maintaining heap property after each swapping
            var j = 0
            var index: Int

            do {
                index = 2 * j + 1

                if (index < i) {
                    var indexValue = elements.get(index)
                    val nextIndexValue = elements.get(index + 1)

                    if (index < i - 1 && compare(indexValue, nextIndexValue) < 0) {
                        index++
                        indexValue = nextIndexValue
                    }

                    val jValue = elements.get(j)
                    if (compare(jValue, indexValue) < 0) {
                        // Swap the values
                        elements.set(j, indexValue)
                        elements.set(index, jValue)
                    }
                }

                j = index
            } while (index < i)

            i--
        }
    }
}