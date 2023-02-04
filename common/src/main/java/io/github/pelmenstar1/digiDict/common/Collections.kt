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

inline fun <reified T> Array<out T>.withAddedElement(element: T): Array<T> {
    val size = size

    val newArray = unsafeNewArray<T>(size + 1)
    System.arraycopy(this, 0, newArray, 0, size)
    newArray[size] = element

    return newArray
}

inline fun <reified T> Array<out T>.withRemovedElementAt(index: Int): Array<T> {
    val size = size

    val newArray = unsafeNewArray<T>(size - 1)
    System.arraycopy(this, 0, newArray, 0, index)
    System.arraycopy(this, index + 1, newArray, index, size - (index + 1))

    return newArray
}

fun IntArray.contains(element: Int, start: Int, end: Int): Boolean {
    for (i in start until end) {
        if (this[i] == element) {
            return true
        }
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