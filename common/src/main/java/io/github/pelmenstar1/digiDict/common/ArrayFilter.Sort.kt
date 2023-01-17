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

    HeapSort.sort(origin, size, Array<T>::get, Array<T>::set, comparator::compare)
    return FilteredArray(origin, size)
}
