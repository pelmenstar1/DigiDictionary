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

    val map = IntArray(origin.size)

    initMap(bitSet, map)
    buildMaxHeap(origin, bitSet, map, comparator)

    val firstSetBitPos = bitSet.firstSetBit()
    var i = arraySize - 1

    bitSet.iterateSetBitsFromEndExceptFirst { bitIndex ->
        // Swap value of first indexed with last indexed
        map.swapValuesAt(firstSetBitPos, bitIndex)

        // Maintaining heap property after each swapping
        var j = 0
        var jBitPos = firstSetBitPos

        var index: Int

        do {
            index = 2 * j + 1

            if (index < i) {
                var indexBitPos = bitSet.findPositionOfNthSetBit(index)
                val nextIndexBitPos = bitSet.nextSetBit(fromIndex = indexBitPos + 1)

                var indexValue = origin[map[indexBitPos]]
                val nextIndexValue = origin[map[nextIndexBitPos]]

                // If left child is smaller than right child point index variable to right child.
                if (index < i - 1 && comparator.compare(indexValue, nextIndexValue) < 0) {
                    index++
                    indexValue = nextIndexValue
                    indexBitPos = nextIndexBitPos
                }

                // if parent is smaller than child then swapping parent with child having higher value
                if (comparator.compare(origin[map[jBitPos]], indexValue) < 0) {
                    map.swapValuesAt(jBitPos, indexBitPos)
                }

                jBitPos = indexBitPos
            }

            j = index
        } while (index < i)

        i--
    }

    return FilteredArray(origin, bitSet, map, arraySize)
}

private fun initMap(bitSet: LongArray, map: IntArray) {
    bitSet.iterateSetBits { bitIndex ->
        map[bitIndex] = bitIndex
    }
}

// Builds Max Heap where value of each child is always smaller than value of their parent.
private fun <T> buildMaxHeap(
    origin: Array<out T>,
    bitSet: LongArray,
    map: IntArray,
    comparator: Comparator<T>
) {
    var i = 1

    bitSet.iterateSetBitsExceptFirst { childBitIndex ->
        val parentBitPos = bitSet.findPositionOfNthSetBit((i - 1) / 2)

        val childIndex = map[childBitIndex]
        val parentIndex = map[parentBitPos]

        // If child is bigger than parent
        if (comparator.compare(origin[childIndex], origin[parentIndex]) > 0) {
            var j = i
            var jBitPos = childBitIndex
            var nextParentBitPos = parentBitPos

            // Swap child and parent until parent is smaller
            while (true) {
                val nextParentIndex = (j - 1) / 2

                if (comparator.compare(origin[map[jBitPos]], origin[map[nextParentBitPos]]) <= 0) {
                    break
                }

                map.swapValuesAt(jBitPos, nextParentBitPos)

                j = nextParentIndex
                jBitPos = nextParentBitPos

                // Calculate nextParentBitIndex for the next iteration.
                // This is done to get rid of one findPositionOfNthSetBit in the first iteration when
                // parentBitPos is used
                nextParentBitPos = bitSet.findPositionOfNthSetBit((j - 1) / 2)
            }
        }

        i++
    }
}

private fun IntArray.swapValuesAt(i: Int, j: Int) {
    val t = this[i]
    this[i] = this[j]
    this[j] = t
}