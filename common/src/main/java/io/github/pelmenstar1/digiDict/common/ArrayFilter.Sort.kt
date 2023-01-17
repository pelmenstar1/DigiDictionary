package io.github.pelmenstar1.digiDict.common

/**
 * Sorts given [FilteredArray] using specified [comparator] to compare values.
 *
 * It's using Heap Sort algorithm which means the sorting is unstable.
 *
 * **Note that the underlying array of given [FilteredArray] is mutated**
 */
fun <T> FilteredArray<T>.sort(comparator: Comparator<in T>) {
    val size = size
    val origin = origin

    HeapSort.sort(origin, size, Array<T>::get, Array<T>::set, comparator::compare)
}
