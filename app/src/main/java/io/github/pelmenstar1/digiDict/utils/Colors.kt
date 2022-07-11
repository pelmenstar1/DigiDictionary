package io.github.pelmenstar1.digiDict.utils

import androidx.annotation.IntRange

/**
 * Returns a color with same R, G, B and specified [alpha]
 */
fun Int.withAlpha(@IntRange(from = 0L, to = 255L) alpha: Int): Int {
    return (this and 0x00FFFFFF) or (alpha shl 24)
}

/**
 * Returns a color with same R, G, B and 0 alpha
 */
fun Int.transparent(): Int {
    return this and 0x00FFFFFF
}