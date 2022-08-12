package io.github.pelmenstar1.digiDict.common.ui

import android.content.Context
import android.graphics.Canvas
import android.graphics.Rect
import android.graphics.drawable.Drawable
import android.view.View
import androidx.core.content.res.getDrawableOrThrow
import androidx.recyclerview.widget.RecyclerView
import kotlin.math.roundToInt

class AdvancedDividerItemDecoration(context: Context) : RecyclerView.ItemDecoration() {
    private val dividerDrawable: Drawable

    init {
        val a = context.obtainStyledAttributes(attrs)

        try {
            dividerDrawable = a.getDrawableOrThrow(0)
        } finally {
            a.recycle()
        }
    }

    override fun onDraw(c: Canvas, parent: RecyclerView, state: RecyclerView.State) {
        var isSaved = false

        val left: Int
        val right: Int
        val parentWidth = parent.width

        if (parent.clipToPadding) {
            left = parent.paddingLeft
            right = parentWidth - parent.paddingRight

            isSaved = true
            c.save()
            c.clipRect(
                left,
                parent.paddingTop,
                right,
                parent.height - parent.paddingBottom
            )
        } else {
            left = 0
            right = parentWidth
        }

        val bounds = tempBounds
        val childCount = parent.childCount
        val divider = dividerDrawable
        val dividerHeight = divider.intrinsicHeight

        for (i in 0 until (childCount - 1)) {
            val child = parent.getChildAt(i)
            parent.getDecoratedBoundsWithMargins(child, bounds)

            val bottom = bounds.bottom + child.translationY.roundToInt()
            val top = bottom - dividerHeight

            divider.setBounds(left, top, right, bottom)
            divider.draw(c)
        }

        if (isSaved) {
            c.restore()
        }
    }

    override fun getItemOffsets(outRect: Rect, view: View, parent: RecyclerView, state: RecyclerView.State) {
        val adapter = parent.adapter
        val isLast = adapter == null || parent.getChildAdapterPosition(view) == adapter.itemCount - 1

        if (isLast) {
            outRect.setEmpty()
        } else {
            outRect.set(0, 0, 0, dividerDrawable.intrinsicHeight)
        }
    }

    companion object {
        private val attrs = intArrayOf(android.R.attr.listDivider)

        private val tempBounds = Rect()
    }
}