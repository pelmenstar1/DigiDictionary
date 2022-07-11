package io.github.pelmenstar1.digiDict.utils

import java.nio.ByteBuffer

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