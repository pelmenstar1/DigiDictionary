package io.github.pelmenstar1.digiDict.common.ui

import android.content.Context
import android.util.AttributeSet
import android.view.ViewGroup
import androidx.annotation.AttrRes
import androidx.annotation.StyleRes
import io.github.pelmenstar1.digiDict.common.getLazyValue
import kotlin.math.max

/**
 * Represents a special version of linear-layout which orientation is horizontal.
 * If there's no space for the view, the view is moved to the next row (or line).
 *
 * The layout doesn't have its own layout params and utilizes [ViewGroup.MarginLayoutParams].
 */
open class MultilineHorizontalLinearLayout @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    @AttrRes defStyleAttr: Int = 0,
    @StyleRes defStyleRes: Int = 0
) : ViewGroup(context, attrs, defStyleAttr, defStyleRes) {
    private class RowMeasurementResult {
        @JvmField
        var endIndex = -1

        @JvmField
        var width = -1

        @JvmField
        var height = -1
    }

    private var _cachedRowMeasurementResult: RowMeasurementResult? = null
    private val cachedRowMeasurementResult: RowMeasurementResult
        get() = getLazyValue(
            _cachedRowMeasurementResult,
            { RowMeasurementResult() }
        ) { _cachedRowMeasurementResult = it }

    var horizontalAlignment: Int = HORIZONTAL_ALIGNMENT_START
        set(value) {
            field = value

            requestLayout()
        }

    var rowVerticalAlignment: Int = VERTICAL_ROW_ALIGNMENT_TOP
        set(value) {
            field = value

            requestLayout()
        }

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
        val totalWidth = if (widthMode == MeasureSpec.EXACTLY || widthMode == MeasureSpec.AT_MOST) {
            MeasureSpec.getSize(widthMeasureSpec)
        } else {
            // As widthMode is MeasureSpec.UNSPECIFIED, there's no limitation for width and therefore there should be only
            // one row.
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

                rowHeight = max(rowHeight, requiredChildHeight)
                childState = combineMeasuredStates(childState, child.measuredState)

                val nextWidth = consumedWidthInRow + requiredChildWidth

                // If there's no space for the current view, allocate space for the next row.
                if (nextWidth >= totalWidthWithoutRightPadding) {
                    requiredWidth = max(requiredWidth, consumedWidthInRow)
                    requiredHeight += rowHeight

                    consumedWidthInRow = pLeft + requiredChildWidth
                    rowHeight = requiredChildHeight
                } else {
                    consumedWidthInRow = nextWidth
                    requiredWidth = max(requiredWidth, consumedWidthInRow)
                }
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
        val rowResult = cachedRowMeasurementResult
        val horizontalAlign = horizontalAlignment
        val pLeft = paddingLeft
        val totalWidthWithoutSidePaddings = measuredWidth - pLeft - paddingRight

        var currentTop = paddingTop
        var childIndex = 0

        while (childIndex < childCount) {
            measureRow(childIndex, rowResult)
            val endIndex = rowResult.endIndex

            val initialLeft = when (horizontalAlign) {
                HORIZONTAL_ALIGNMENT_START -> pLeft
                HORIZONTAL_ALIGNMENT_CENTER -> (totalWidthWithoutSidePaddings - rowResult.width) / 2
                else -> 0
            }

            layoutRow(childIndex, endIndex, initialLeft, currentTop, rowResult)

            currentTop += rowResult.height
            childIndex = endIndex
        }
    }

    private fun measureRow(startIndex: Int, outResult: RowMeasurementResult) {
        if (startIndex == childCount) {
            return
        }

        val pLeft = paddingLeft
        val totalWidthWithoutRightPadding = measuredWidth - paddingRight

        val startViewLp = getChildAt(startIndex).layoutParams as MarginLayoutParams

        var currentLeft = pLeft
        var rowHeight = 0

        var lastInRowLp: MarginLayoutParams? = null
        var endIndex = childCount

        for (i in startIndex until childCount) {
            val child = getChildAt(i)

            if (child.visibility != GONE) {
                val childParams = child.layoutParams as MarginLayoutParams

                val requiredWidth = child.measuredWidth + childParams.leftMargin + childParams.rightMargin
                val requiredHeight = child.measuredHeight + childParams.topMargin + childParams.bottomMargin

                rowHeight = max(rowHeight, requiredHeight)

                val nextLeft = currentLeft + requiredWidth

                if (nextLeft > totalWidthWithoutRightPadding) {
                    endIndex = i

                    // Use layout params of previous view because current view won't be in current row
                    lastInRowLp = getChildAt(i - 1).layoutParams as MarginLayoutParams

                    break
                }

                currentLeft = nextLeft
            }
        }

        // In the case, row is the last one
        if (lastInRowLp == null) {
            lastInRowLp = getChildAt(childCount - 1).layoutParams as MarginLayoutParams
        }

        outResult.endIndex = endIndex

        // Don't take into account left padding of the layout, left margin of start view and right margin
        // of last view in the row to get more accurate row width
        outResult.width = currentLeft - pLeft - startViewLp.leftMargin - lastInRowLp.rightMargin
        outResult.height = rowHeight
    }

    private fun layoutRow(
        startViewIndex: Int,
        endViewIndex: Int,
        initialLeft: Int,
        initialTop: Int,
        rowResult: RowMeasurementResult
    ) {
        var currentLeft = initialLeft

        val horizontalAlign = horizontalAlignment
        val verticalAlign = rowVerticalAlignment

        val rowHeight = rowResult.height

        for (i in startViewIndex until endViewIndex) {
            val child = getChildAt(i)

            if (child.visibility != GONE) {
                val childParams = child.layoutParams as MarginLayoutParams
                val childLeftMargin = childParams.leftMargin
                val childTopMargin = childParams.topMargin

                val childWidth = child.measuredWidth
                val childHeight = child.measuredHeight

                var requiredWidth = childWidth + childParams.rightMargin

                // Don't use left margin on first view in row when center alignment is used
                if (horizontalAlign != HORIZONTAL_ALIGNMENT_CENTER || i != startViewIndex) {
                    requiredWidth += childLeftMargin
                }

                val childLeft = currentLeft + childLeftMargin
                var childTop = initialTop

                when (verticalAlign) {
                    VERTICAL_ROW_ALIGNMENT_TOP -> {
                        childTop += childTopMargin
                    }
                    VERTICAL_ROW_ALIGNMENT_CENTER -> {
                        childTop += (rowHeight - childHeight) / 2
                    }
                }

                child.layout(childLeft, childTop, childLeft + childWidth, childTop + childHeight)

                currentLeft += requiredWidth
            }
        }
    }

    companion object {
        const val HORIZONTAL_ALIGNMENT_START = 0
        const val HORIZONTAL_ALIGNMENT_CENTER = 1

        const val VERTICAL_ROW_ALIGNMENT_TOP = 0
        const val VERTICAL_ROW_ALIGNMENT_CENTER = 1
    }
}