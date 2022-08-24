package io.github.pelmenstar1.digiDict.common.ui

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View
import androidx.annotation.AttrRes
import androidx.annotation.StyleRes

class ColorCircleView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    @AttrRes defStyleAttr: Int = 0,
    @StyleRes defStyleRes: Int = 0
) : View(context, attrs, defStyleAttr, defStyleRes) {
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }

    var color: Int = 0
        set(value) {
            if (field != value) {
                field = value
                paint.color = value

                invalidate()
            }
        }

    override fun onDraw(canvas: Canvas) {
        canvas.drawOval(0f, 0f, width.toFloat(), height.toFloat(), paint)
    }
}