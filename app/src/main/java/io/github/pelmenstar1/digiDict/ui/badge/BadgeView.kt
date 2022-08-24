package io.github.pelmenstar1.digiDict.ui.badge

import android.content.Context
import android.graphics.Canvas
import com.google.android.material.textview.MaterialTextView
import io.github.pelmenstar1.digiDict.R
import io.github.pelmenstar1.digiDict.data.RecordBadgeInfo

// Instantiated only from the code.
class BadgeView(context: Context) : MaterialTextView(context) {
    private val outlineHelper = BadgeOutlineHelper(context)

    var badge: RecordBadgeInfo? = null
        set(value) {
            field = value

            if (value != null) {
                text = value.name
                outlineHelper.setOutlineColor(value.outlineColor)
            }
        }

    init {
        val res = context.resources

        val horizontalPadding = res.getDimensionPixelOffset(R.dimen.badge_paddingHorizontal)
        val verticalPadding = res.getDimensionPixelOffset(R.dimen.badge_paddingVertical)

        setPadding(
            horizontalPadding,
            verticalPadding,
            horizontalPadding,
            verticalPadding
        )

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