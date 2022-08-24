package io.github.pelmenstar1.digiDict.ui.badge

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import androidx.annotation.ColorInt
import io.github.pelmenstar1.digiDict.R

class BadgeOutlineHelper(context: Context) {
    private val strokeWidth: Float
    private var outlineRoundRadius = 0f
    private val outlinePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
    }

    private var width = 0f
    private var height = 0f

    init {
        strokeWidth = context.resources.getDimension(R.dimen.badge_strokeWidth)
        outlinePaint.strokeWidth = strokeWidth
    }

    fun setOutlineColor(@ColorInt color: Int) {
        outlinePaint.color = color
    }

    fun onSizeChanged(w: Int, h: Int) {
        val fh = h.toFloat()

        width = w.toFloat()
        height = fh
        outlineRoundRadius = fh * 0.5f
    }

    fun draw(canvas: Canvas) {
        val hsw = strokeWidth * 0.5f
        val rr = outlineRoundRadius

        canvas.drawRoundRect(hsw, hsw, width - hsw, height - hsw, rr, rr, outlinePaint)
    }
}