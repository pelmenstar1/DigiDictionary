package io.github.pelmenstar1.digiDict.ui.record

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.util.AttributeSet
import android.widget.LinearLayout
import androidx.annotation.AttrRes
import androidx.annotation.StyleRes

class RecordItemRootContainer @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    @AttrRes defStyleAttr: Int = 0,
    @StyleRes defStyleRes: Int = 0
) : LinearLayout(context, attrs, defStyleAttr, defStyleRes) {
    private val dividerPaint = Paint(Paint.ANTI_ALIAS_FLAG)

    var dividerColor = 0
        set(value) {
            field = value

            dividerPaint.color = value
        }

    var dividerHeight = 0f

    var hasDivider: Boolean = true
        set(value) {
            field = value

            invalidate()
        }

    init {
        setWillNotDraw(false)
    }

    override fun onDraw(c: Canvas) {
        super.onDraw(c)

        if (hasDivider) {
            val w = width
            val h = height.toFloat()

            c.drawRect(0f, h - dividerHeight, w.toFloat(), h, dividerPaint)
        }
    }
}