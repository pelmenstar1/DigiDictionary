package io.github.pelmenstar1.digiDict.common

import android.graphics.Color
import androidx.annotation.ColorInt
import androidx.core.graphics.alpha
import androidx.core.graphics.blue
import androidx.core.graphics.green
import androidx.core.graphics.red

fun lerp(start: Float, end: Float, fraction: Float): Float {
    return (1f - fraction) * start + fraction * end
}

fun lerp(start: Int, end: Int, fraction: Float): Int {
    // Rounds to the nearest integer
    return (lerp(start.toFloat(), end.toFloat(), fraction) + 0.5f).toInt()
}

fun lerpRgb(@ColorInt start: Int, @ColorInt end: Int, fraction: Float): Int {
    return Color.argb(
        lerp(start.alpha, end.alpha, fraction),
        lerp(start.red, end.red, fraction),
        lerp(start.green, end.green, fraction),
        lerp(start.blue, end.blue, fraction)
    )
}