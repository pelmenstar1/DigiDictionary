package io.github.pelmenstar1.digiDict.common.android

import android.graphics.Canvas
import android.graphics.ColorFilter
import android.graphics.Paint
import android.graphics.PixelFormat
import android.graphics.drawable.Drawable
import kotlin.math.min

/**
 * A [Drawable] that represents a rectangle whose corners are as round as possible with given size.
 *
 * By default, the rectangle will be filled but it can be changed using [style]
 */
class MaxRoundRectDrawable : Drawable() {
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)

    var style: Paint.Style
        get() = paint.style
        set(value) {
            paint.style = value

            invalidateSelf()
        }

    var color: Int
        get() = paint.color
        set(value) {
            paint.color = value

            invalidateSelf()
        }

    var strokeWidth: Float
        get() = paint.strokeWidth
        set(value) {
            paint.strokeWidth = value

            invalidateSelf()
        }

    override fun getAlpha(): Int {
        return paint.alpha
    }

    override fun setAlpha(alpha: Int) {
        paint.alpha = alpha

        invalidateSelf()
    }

    override fun getColorFilter(): ColorFilter? = paint.colorFilter

    override fun setColorFilter(colorFilter: ColorFilter?) {
        paint.colorFilter = colorFilter

        invalidateSelf()
    }

    @Deprecated("Deprecated in Java")
    override fun getOpacity(): Int {
        if (paint.colorFilter != null) {
            return PixelFormat.TRANSLUCENT
        }

        return when (paint.alpha) {
            0 -> PixelFormat.TRANSPARENT
            255 -> PixelFormat.OPAQUE
            else -> PixelFormat.TRANSLUCENT
        }
    }

    override fun draw(canvas: Canvas) {
        val bounds = bounds
        val roundRadius = min(bounds.width(), bounds.height()) * 0.5f

        val left = bounds.left.toFloat()
        val top = bounds.top.toFloat()
        val right = bounds.right.toFloat()
        val bottom = bounds.bottom.toFloat()

        if (paint.style == Paint.Style.FILL) {
            canvas.drawRoundRect(left, top, right, bottom, roundRadius, roundRadius, paint)
        } else {
            val sw = paint.strokeWidth * 0.5f

            canvas.drawRoundRect(
                left + sw, top + sw, right - sw, bottom - sw,
                roundRadius, roundRadius,
                paint
            )
        }
    }
}