package io.github.pelmenstar1.digiDict.common

inline fun <TArray, TValue> TArray.swap(
    i: Int, j: Int,
    get: TArray.(Int) -> TValue,
    set: TArray.(Int, TValue) -> Unit
) {
    val t = get(i)
    set(i, get(j))
    set(j, t)
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

inline fun <T, reified R> Array<out T>.mapToArrayIndexed(block: (index: Int, T) -> R): Array<R> {
    return Array(size) { i -> block(i, this[i]) }
}

inline fun <T> Array<out T>.mapToIntArray(block: (T) -> Int): IntArray {
    return IntArray(size) { block(this[it]) }
}

/**
 * Same as [Iterable.forEach] but does not create an [Iterator] and directly accesses the elements of the [List].
 */
@Suppress("ReplaceManualRangeWithIndicesCalls")
inline fun <T> List<T>.forEachWithNoIterator(block: (T) -> Unit) {
    for (i in 0 until size) {
        block(this[i])
    }
}

fun IntArray.sum(start: Int = 0, end: Int = size): Int {
    var acc = 0
    for (i in start until end) {
        acc += this[i]
    }

    return acc
}