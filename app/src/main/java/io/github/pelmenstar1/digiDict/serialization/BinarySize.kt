package io.github.pelmenstar1.digiDict.serialization

import android.database.CharArrayBuffer

/**
 * Convenient helper to compute byte size of primary types.
 */
object BinarySize {
    const val int32 = 4
    const val int64 = 8

    fun stringUtf16(value: String): Int {
        // +1 for \0 terminator
        return value.length * 2 + 1
    }

    fun stringUtf16(buffer: CharArrayBuffer): Int {
        return buffer.sizeCopied * 2 + 1
    }
}