package io.github.pelmenstar1.digiDict.common

/**
 * Returns new [FilteredArray] that represents sorted given [FilteredArray] using specified [comparator] to compare values.
 *
 * It's using Heap Sort algorithm which means the sorting is unstable.
 * The method does not modify given [FilteredArray].
 */
fun <T> FilteredArray<T>.sorted(comparator: Comparator<T>): FilteredArray<T> {
    val arraySize = size

    if (arraySize == 0) {
        return FilteredArray.empty()
    }

    // The algorithm is modified a bit because FilteredArray is special in that a bit array determines which
    // element passed filtering and which doesn't. The usage of bit array makes each random access linear-time that
    // is a substantial overhead. The algorithm tries to diminish that overhead.

    val bitSet = bitSet
    val origin = origin

    val map = IntArray(arraySize) { it }

    buildMaxHeap(origin, bitSet, map, comparator)

    for (i in (size - 1) downTo 1) {
        // Swap value of first indexed with last indexed
        map.swapValuesAt(0, i)

        // Maintaining heap property after each swapping
        var j = 0
        var index: Int

        do {
            index = 2 * j + 1

            if (index < i) {
                val indexBitPos = bitSet.findPositionOfNthSetBit(map[index])
                val nextIndexBitPos = bitSet.findPositionOfNthSetBit(map[index + 1])

                var indexValue = origin[indexBitPos]
                val nextIndexValue = origin[nextIndexBitPos]

                // If left child is smaller than right child point index variable to right child.
                if (index < i - 1 && comparator.compare(indexValue, nextIndexValue) < 0) {
                    index++
                    indexValue = nextIndexValue
                }

                val jBitPos = bitSet.findPositionOfNthSetBit(map[j])

                // if parent is smaller than child then swapping parent with child having higher value
                if (comparator.compare(origin[jBitPos], indexValue) < 0) {
                    map.swapValuesAt(j, index)
                }
            }

            j = index
        } while (index < i)
    }

    return FilteredArray(origin, bitSet, map, arraySize)
}

// Builds Max Heap where value of each child is always smaller than value of their parent.
private fun <T> buildMaxHeap(
    origin: Array<out T>,
    bitSet: LongArray,
    map: IntArray,
    comparator: Comparator<T>
) {
    for (i in 1 until map.size) {
        val childBitIndex = bitSet.findPositionOfNthSetBit(map[i])
        val parentBitIndex = bitSet.findPositionOfNthSetBit(map[(i - 1) / 2])

        // If child is bigger than parent
        if (comparator.compare(origin[childBitIndex], origin[parentBitIndex]) > 0) {
            var j = i

            // Swap child and parent until parent is smaller
            while (true) {
                val nextParentIndex = (j - 1) / 2

                val jBitPos = bitSet.findPositionOfNthSetBit(map[j])
                val nextParentBitPos = bitSet.findPositionOfNthSetBit(map[nextParentIndex])

                if (comparator.compare(origin[jBitPos], origin[nextParentBitPos]) <= 0) {
                    break
                }

                map.swapValuesAt(j, nextParentIndex)

                j = nextParentIndex
            }
        }
    }
}

private fun IntArray.swapValuesAt(i: Int, j: Int) {
    val t = this[i]
    this[i] = this[j]
    this[j] = t
}