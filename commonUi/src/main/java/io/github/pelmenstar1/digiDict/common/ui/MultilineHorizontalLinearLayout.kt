package io.github.pelmenstar1.digiDict.common.ui

import android.content.Context
import android.util.AttributeSet
import android.view.ViewGroup
import androidx.annotation.AttrRes
import androidx.annotation.StyleRes
import kotlin.math.max

/**
 * Represents a special version of linear-layout which orientation is horizontal.
 * If there's no space for the view, the view is moved to the next row (or line).
 *
 * The layout doesn't have its own [ViewGroup.LayoutParams] and utilizes [ViewGroup.MarginLayoutParams].
 */
class MultilineHorizontalLinearLayout @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    @AttrRes defStyleAttr: Int = 0,
    @StyleRes defStyleRes: Int = 0
) : ViewGroup(context, attrs, defStyleAttr, defStyleRes) {
    override fun generateDefaultLayoutParams(): LayoutParams {
        return MarginLayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT)
    }

    override fun generateLayoutParams(attrs: AttributeSet?): LayoutParams = MarginLayoutParams(context, attrs)
    override fun generateLayoutParams(p: LayoutParams?): LayoutParams = MarginLayoutParams(p)

    override fun checkLayoutParams(p: LayoutParams?) = p is MarginLayoutParams
    override fun shouldDelayChildPressedState() = false
    override fun getAccessibilityClassName(): String = MultilineHorizontalLinearLayout::class.java.name

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val widthMode = MeasureSpec.getMode(widthMeasureSpec)
        val totalWidth = if (widthMode == MeasureSpec.EXACTLY || widthMode == MeasureSpec.EXACTLY) {
            MeasureSpec.getSize(widthMeasureSpec)
        } else {
            // As widthMode is MeasureSpec.UNSPECIFIED, there's no limitation for width and therefore there should be only
            // one row. To make that happen, totalWidth should be max 32-bit integer.
            Int.MAX_VALUE
        }

        val pLeft = paddingLeft
        val pRight = paddingRight

        val totalWidthWithoutRightPadding = totalWidth - pRight

        var consumedWidthInRow = 0
        var rowHeight = 0

        // If there's no views, paddings should be taken into account anyway.
        var requiredWidth = pLeft + pRight

        // Row heights will be added to this variable and vertical paddings should be here in the first place.
        var requiredHeight = paddingTop + paddingBottom
        var childState = 0

        for (i in 0 until childCount) {
            val child = getChildAt(i)

            if (child.visibility != GONE) {
                measureChildWithMargins(child, widthMeasureSpec, 0, heightMeasureSpec, 0)

                val lp = child.layoutParams as MarginLayoutParams

                val childWidth = child.measuredWidth
                val childHeight = child.measuredHeight

                val requiredChildWidth = childWidth + lp.leftMargin + lp.rightMargin
                val requiredChildHeight = childHeight + lp.topMargin + lp.bottomMargin

                childState = combineMeasuredStates(childState, child.measuredState)

                val nextWidth = consumedWidthInRow + requiredChildWidth

                // If there's no space for the current view, allocate space for the next row.
                if (nextWidth > totalWidthWithoutRightPadding) {
                    requiredWidth = max(requiredWidth, consumedWidthInRow)
                    requiredHeight += rowHeight

                    // Reset the counters.
                    consumedWidthInRow = pLeft
                    rowHeight = 0
                } else {
                    consumedWidthInRow = nextWidth
                    requiredWidth = max(requiredWidth, consumedWidthInRow)
                }

                rowHeight = max(rowHeight, requiredChildHeight)
            }
        }

        // Add the last row height.
        requiredHeight += rowHeight

        requiredWidth = max(requiredWidth, suggestedMinimumWidth)
        requiredHeight = max(requiredHeight, suggestedMinimumHeight)

        setMeasuredDimension(
            resolveSizeAndState(requiredWidth, widthMeasureSpec, childState),
            resolveSizeAndState(requiredHeight, heightMeasureSpec, childState)
        )
    }

    override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
        val totalWidth = measuredWidth
        val pLeft = paddingLeft
        val totalWidthWithoutRightPadding = totalWidth - paddingRight

        var currentTop = paddingTop
        var currentLeft = pLeft

        var rowHeight = 0

        for (i in 0 until childCount) {
            val child = getChildAt(i)

            if (child.visibility != GONE) {
                val childParams = child.layoutParams as MarginLayoutParams
                val childLeftMargin = childParams.leftMargin
                val childTopMargin = childParams.topMargin

                val childWidth = child.measuredWidth
                val childHeight = child.measuredHeight

                val requiredWidth = childWidth + childLeftMargin + childParams.rightMargin
                var nextLeft = currentLeft + requiredWidth

                if (nextLeft > totalWidthWithoutRightPadding) {
                    currentTop += rowHeight
                    currentLeft = pLeft
                    nextLeft = pLeft + requiredWidth

                    rowHeight = 0
                }

                val requiredHeight = childHeight + childTopMargin + childParams.bottomMargin
                rowHeight = max(rowHeight, requiredHeight)

                val leftWithMargin = currentLeft + childLeftMargin
                val topWithMargin = currentTop + childTopMargin

                child.layout(leftWithMargin, topWithMargin, leftWithMargin + childWidth, topWithMargin + childHeight)
                currentLeft = nextLeft
            }
        }
    }
}