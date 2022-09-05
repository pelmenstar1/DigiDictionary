package io.github.pelmenstar1.digiDict.common.binarySerialization

import android.database.CharArrayBuffer

/**
 * Convenient helper to compute byte size of primary types.
 */
object BinarySize {
    const val int32 = 4
    const val int64 = 8

    fun stringUtf16(value: String): Int {
        // +2 for length prefix.
        return value.length * 2 + 2
    }

    fun stringUtf16(buffer: CharArrayBuffer): Int {
        // +2 for length prefix.
        return buffer.sizeCopied * 2 + 2
    }
}