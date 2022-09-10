package io.github.pelmenstar1.digiDict.common

import androidx.collection.ArraySet

interface SizedIterable<out T> : Iterable<T> {
    val size: Int
}

fun <T> Array<out T>.asSizedIterable(): SizedIterable<T> {
    val array = this

    return object : SizedIterable<T> {
        override val size: Int
            get() = array.size

        override fun iterator() = array.iterator()
    }
}

/**
 * Represents a class that sequentially adds given elements to the [array] to fulfill it.
 */
class LocalArrayBuilder<T>(val array: Array<T>) {
    private var index = 0

    fun add(value: T) {
        array[index++] = value
    }

    fun isFull() = index == array.size
}

/**
 * A fast-path for instantiating the [LocalArrayBuilder] instance with given size of the array.
 * If [T] is not-null, it's unsafe to use the array until it's fully filled with not-null elements.
 */
@Suppress("UNCHECKED_CAST")
inline fun <reified T> LocalArrayBuilder(size: Int): LocalArrayBuilder<T> {
    return LocalArrayBuilder(arrayOfNulls<T>(size) as Array<T>)
}

@Suppress("UNCHECKED_CAST")
inline fun <reified T> unsafeNewArray(size: Int): Array<T> {
    return arrayOfNulls<T>(size) as Array<T>
}

inline fun <TArray : Any, TOut : TArray> TArray.withAddedElementInternal(
    currentSize: Int,
    createNewArray: (size: Int) -> TOut,
    set: TOut.(index: Int) -> Unit
): TOut {
    val newArray = createNewArray(currentSize + 1)
    System.arraycopy(this, 0, newArray, 0, currentSize)
    newArray.set(currentSize)

    return newArray
}

inline fun <TArray : Any, TOut : TArray> TArray.withAddedElementsInternal(
    elementsToAdd: TArray,
    createNewArray: (size: Int) -> TOut,
    getSize: TArray.() -> Int,
): TOut {
    val currentSize = getSize()
    val elementsSize = elementsToAdd.getSize()

    val newArray = createNewArray(currentSize + elementsSize)
    System.arraycopy(this, 0, newArray, 0, currentSize)
    System.arraycopy(elementsToAdd, 0, newArray, currentSize, elementsSize)

    return newArray
}

inline fun <TArray : Any, TOut : TArray> TArray.withRemovedElementAtInternal(
    index: Int,
    createNewArray: (size: Int) -> TOut,
    getSize: TArray.() -> Int
): TOut {
    val size = getSize()
    val newArray = createNewArray(size - 1)
    System.arraycopy(this, 0, newArray, 0, index)
    System.arraycopy(this, index + 1, newArray, index, size - (index + 1))

    return newArray
}

inline fun <reified T> Array<out T>.withAddedElement(element: T): Array<T> {
    return withAddedElementInternal(size, ::unsafeNewArray) { this[it] = element }
}

fun IntArray.withAddedElement(element: Int): IntArray {
    return withAddedElementInternal(size, ::IntArray) { this[it] = element }
}

fun IntArray.withAddedElements(elements: IntArray): IntArray {
    return withAddedElementsInternal(elements, ::IntArray, IntArray::size)
}

inline fun <reified T> Array<out T>.withRemovedElementAt(index: Int): Array<T> {
    return withRemovedElementAtInternal(index, ::unsafeNewArray, Array<out T>::size)
}

fun IntArray.withRemovedElementAt(index: Int): IntArray {
    return withRemovedElementAtInternal(index, ::IntArray, IntArray::size)
}

fun IntArray.contains(element: Int, start: Int, end: Int): Boolean {
    for (i in start until end) {
        if (this[i] == element) return true
    }

    return false
}

fun <T> newArraySetFrom(set: Set<T>, capacity: Int): ArraySet<T> {
    return ArraySet<T>(capacity).also {
        it.addAllSet(set)
    }
}

fun <T> ArraySet<in T>.addAllSet(set: Set<T>) {
    // ArraySet has addAll(ArraySet) method, it's more optimized because it uses internal specialties of the class.
    // ArraySet also has addAll(Collection) method, it's less optimized, it uses iterator and etc.
    //
    // So in order to use more optimized overload, Kotlin should be convinced that set is ArraySet.
    if (set is ArraySet<out T>) {
        addAll(set)
    } else {
        addAll(set)
    }
}

fun <T> MutableSet<in T>.addAllArray(elements: Array<out T>) {
    elements.forEach(::add)
}

@Suppress("UNCHECKED_CAST")
inline fun <T> Set<T>.forEachFast(action: (T) -> Unit) {
    if (this is ArraySet<T>) {
        for (i in 0 until size) {
            action(valueAt(i) as T)
        }
    } else {
        forEach(action)
    }
}

inline fun <T, R> Array<out T>.mapOffset(offset: Int, block: (T) -> R): List<R> {
    val resultSize = size - offset

    when {
        offset < 0 -> throw IllegalArgumentException("offset is negative")
        resultSize < 0 -> throw IllegalArgumentException("size < offset")
        resultSize == 0 -> return emptyList()
    }

    val result = ArrayList<R>(resultSize)

    for (i in offset until size) {
        result.add(block(this[i]))
    }

    return result
}


inline fun <T, reified R> Array<out T>.mapToArray(block: (T) -> R): Array<R> {
    return Array(size) { block(this[it]) }
}

inline fun <T> Array<out T>.mapToIntArray(block: (T) -> Int): IntArray {
    return IntArray(size) { block(this[it]) }
}

inline fun <T> List<T>.forEachWithNoIterator(block: (T) -> Unit) {
    for (i in 0 until size) {
        block(this[i])
    }
}