package io.github.pelmenstar1.digiDict.ui.badge

import android.content.Context
import android.graphics.Canvas
import com.google.android.material.textview.MaterialTextView
import io.github.pelmenstar1.digiDict.R
import io.github.pelmenstar1.digiDict.data.RecordBadgeInfo

// Instantiated only from the code.
class BadgeView(context: Context) : MaterialTextView(context) {
    private val outlineHelper = BadgeOutlineHelper(context)

    private var _badge: RecordBadgeInfo? = null
    var badge: RecordBadgeInfo
        get() = requireNotNull(_badge)
        set(value) {
            if (_badge != value) {
                _badge = value

                text = value.name
                outlineHelper.setOutlineColor(value.outlineColor)

                invalidate()
            }
        }

    init {
        with(context.resources) {
            val horizontalPadding = getDimensionPixelOffset(R.dimen.badge_paddingHorizontal)
            val verticalPadding = getDimensionPixelOffset(R.dimen.badge_paddingVertical)

            setPadding(horizontalPadding, verticalPadding, horizontalPadding, verticalPadding)
        }

        ellipsize = null
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        outlineHelper.onSizeChanged(w, h)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        outlineHelper.draw(canvas)
    }
}