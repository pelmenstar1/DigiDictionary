package io.github.pelmenstar1.digiDict.utils

import java.nio.ByteBuffer

@Suppress("UNCHECKED_CAST")
inline fun <reified T> unsafeNewArray(size: Int): Array<T> {
    return arrayOfNulls<T>(size) as Array<T>
}

@Suppress("UNCHECKED_CAST")
inline fun<reified T> Array<T>.withAddedElement(element: T): Array<T> {
    val newArray = unsafeNewArray<T>(size + 1)
    System.arraycopy(this, 0, newArray, 0, size)
    newArray[size] = element

    return newArray
}

@Suppress("UNCHECKED_CAST")
inline fun <reified T> Array<T>.withRemovedElementAt(index: Int): Array<T> {
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