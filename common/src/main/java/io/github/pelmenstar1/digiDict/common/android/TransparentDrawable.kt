package io.github.pelmenstar1.digiDict.common.android

import android.graphics.Canvas
import android.graphics.ColorFilter
import android.graphics.PixelFormat
import android.graphics.drawable.Drawable

object TransparentDrawable : Drawable() {
    override fun draw(canvas: Canvas) {
    }

    override fun setAlpha(alpha: Int) {
    }

    override fun setColorFilter(colorFilter: ColorFilter?) {
    }

    override fun getAlpha() = 0

    @Suppress("OVERRIDE_DEPRECATION")
    override fun getOpacity() = PixelFormat.TRANSPARENT
}