package io.github.pelmenstar1.digiDict.utils

import androidx.collection.ArraySet
import java.nio.ByteBuffer

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

@Suppress("UNCHECKED_CAST")
inline fun <reified T> unsafeNewArray(size: Int): Array<T> {
    return arrayOfNulls<T>(size) as Array<T>
}

@Suppress("UNCHECKED_CAST")
inline fun <reified T> Array<out T>.withAddedElement(element: T): Array<T> {
    val newArray = unsafeNewArray<T>(size + 1)
    System.arraycopy(this, 0, newArray, 0, size)
    newArray[size] = element

    return newArray
}

@Suppress("UNCHECKED_CAST")
inline fun <reified T> Array<out T>.withRemovedElementAt(index: Int): Array<T> {
    val newArray = unsafeNewArray<T>(size - 1)
    System.arraycopy(this, 0, newArray, 0, index)
    System.arraycopy(this, index + 1, newArray, index, size - (index + 1))

    return newArray
}

fun IntArray.contains(element: Int, start: Int, end: Int): Boolean {
    for (i in start until end) {
        if (this[i] == element) return true
    }

    return false
}

fun ByteArray.indexOf(element: Byte, start: Int, end: Int, step: Int = 1): Int {
    var i = start
    while (i < end) {
        if (this[i] == element) return i

        i += step
    }

    return -1
}

fun ByteBuffer.indexOf(element: Byte, step: Int = 1): Int {
    var i = position()
    val end = limit()

    while (i < end) {
        if (get(i) == element) return i

        i += step
    }

    return -1
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

fun <T, R> Array<out T>.mapOffset(offset: Int, block: (T) -> R): List<R> {
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