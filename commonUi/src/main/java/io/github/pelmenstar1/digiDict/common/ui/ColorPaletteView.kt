package io.github.pelmenstar1.digiDict.common.ui

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.*
import android.os.Build
import android.os.Parcel
import android.os.Parcelable
import android.util.AttributeSet
import android.view.AbsSavedState
import android.view.MotionEvent
import android.view.View
import androidx.annotation.AttrRes
import androidx.annotation.ColorInt
import androidx.annotation.StyleRes
import androidx.core.graphics.withSave
import androidx.core.graphics.withTranslation
import io.github.pelmenstar1.digiDict.common.*
import io.github.pelmenstar1.digiDict.common.android.PrimitiveAnimator
import io.github.pelmenstar1.digiDict.common.android.getPrimaryColor
import java.util.*

class ColorPaletteView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    @AttrRes defStyleAttr: Int = 0,
    @StyleRes defStyleRes: Int = 0
) : View(context, attrs, defStyleAttr, defStyleRes) {
    fun interface OnColorSelectedListener {
        fun onSelected(@ColorInt color: Int)
    }

    private class SavedState : AbsSavedState {
        var selectedIndex = -1

        constructor(superState: Parcelable?) : super(superState)
        constructor(source: Parcel) : super(source) {
            selectedIndex = source.readInt()
        }

        override fun writeToParcel(dest: Parcel, flags: Int) {
            super.writeToParcel(dest, flags)

            dest.writeInt(selectedIndex)
        }

        companion object CREATOR : Parcelable.Creator<SavedState> {
            override fun createFromParcel(source: Parcel) = SavedState(source)
            override fun newArray(size: Int) = arrayOfNulls<SavedState>(size)
        }
    }

    @JvmField
    internal val cellSize: Float

    @JvmField
    internal val cellSpacing: Float
    private val cellStrokeWidth: Float

    @JvmField
    internal val cellAreaTopMargin: Float

    @JvmField
    internal val cellAreaBottomMargin: Float

    @JvmField
    internal val cellAreaHorizontalMargin: Float

    @JvmField
    internal var cellsInRow = 0
    private var cellRowStart = 0f

    private val cellFillPaint: Paint
    private val cellStrokePaint: Paint
    private val cellStrokeShadowPaint: Paint

    private var cellStrokeAlpha = 255

    private val outlinePaint: Paint
    private val outlineRoundRadius: Float
    private val outlineStrokeWidth: Float

    private val titlePaint: Paint
    private val titleStartMargin: Float
    private val titleHorizontalPadding: Float

    private var titleWidth = 0f
    private var titleHeight = 0f

    private var selectionAnimPrevCellIndex = -1
    private val selectionAnimator: PrimitiveAnimator

    private var _selectedIndex = -1

    @JvmField
    internal val colors = IntList()

    val selectedIndex: Int
        get() = _selectedIndex

    var title: String = ""
        set(value) {
            field = value

            val rect = Rect()
            titlePaint.getTextBounds(value, 0, value.length, rect)
            titleWidth = rect.width().toFloat()
            titleHeight = rect.height().toFloat()

            invalidate()
        }

    var cellStrokeColor = 0
        set(value) {
            field = value

            cellStrokePaint.color = value

            refreshCellStrokeShadowShader()
            invalidate()
        }

    var onColorSelectedListener: OnColorSelectedListener? = null

    init {
        val res = context.resources

        cellAreaTopMargin = res.getDimension(R.dimen.colorPalette_cellAreaTopMargin)
        cellAreaBottomMargin = res.getDimension(R.dimen.colorPalette_cellAreaBottomMargin)
        cellAreaHorizontalMargin = res.getDimension(R.dimen.colorPalette_cellAreaHorizontalMargin)

        cellSpacing = res.getDimension(R.dimen.colorPalette_cellSpacing)
        cellSize = res.getDimension(R.dimen.colorPalette_cellSize)
        cellStrokeWidth = res.getDimension(R.dimen.colorPalette_cellStrokeWidth)
        outlineRoundRadius = res.getDimension(R.dimen.colorPalette_outlineRoundRadius)
        outlineStrokeWidth = res.getDimension(R.dimen.colorPalette_outlineStrokeWith)
        titleStartMargin = res.getDimension(R.dimen.colorPalette_titleStartMargin)
        titleHorizontalPadding = res.getDimension(R.dimen.colorPalette_titleHorizontalPadding)

        val selAnimDuration = res.getInteger(R.integer.colorPalette_cellSelectionAnimationDuration).toLong()

        val primaryColor = context.getPrimaryColor()

        outlinePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            color = primaryColor
            strokeWidth = outlineStrokeWidth
        }

        titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = primaryColor
            textSize = res.getDimension(R.dimen.colorPalette_titleTextSize)
        }

        cellFillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
        }

        cellStrokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = cellStrokeWidth
        }

        cellStrokeShadowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
        }

        selectionAnimator = PrimitiveAnimator(::onSelectionAnimationTick).apply {
            duration = selAnimDuration
        }

        attrs?.let {
            val a = context.obtainStyledAttributes(it, R.styleable.ColorPaletteView, defStyleAttr, defStyleRes)

            try {
                if (a.hasValue(R.styleable.ColorPaletteView_colors)) {
                    a.getResourceId(R.styleable.ColorPaletteView_colors, 0).also { colorsRes ->
                        addColors(res.getIntArray(colorsRes))
                    }
                }

                cellStrokeColor = a.getColor(R.styleable.ColorPaletteView_strokeColor, 0)

                a.getString(R.styleable.ColorPaletteView_title)?.also { t ->
                    title = t
                }
            } finally {
                a.recycle()
            }
        }
    }

    /**
     * Adds multiple colors to the palette.
     * It's not checked, but all colors in the palette are expected to be unique.
     */
    fun addColors(values: IntArray) {
        colors.addRange(values)

        requestLayout()
        invalidate()
    }

    /**
     * Selects specified [color] in the palette.
     * If specified [color] is not in the palette, selects the last color if there's at least one color.
     */
    fun selectColorOrLast(@ColorInt color: Int, animate: Boolean = true) {
        val index = colors.indexOf(color)
        val colorsLength = colors.size

        if (index >= 0) {
            selectColorAt(index, animate)
        } else if (colorsLength > 0) {
            selectColorAt(colorsLength - 1, animate)
        }
    }

    fun selectLastColor(animate: Boolean = true) {
        val size = colors.size

        if (size > 0) {
            selectColorAt(size - 1, animate)
        }
    }

    fun selectColorAt(index: Int, animate: Boolean = true) {
        if (index < 0 || index >= colors.size) {
            throw IllegalArgumentException("Unable to select a cell at index $index (color count = ${colors.size})")
        }

        val oldIndex = _selectedIndex

        if (oldIndex != index) {
            _selectedIndex = index

            onColorSelectedListener?.onSelected(colors[index])

            if (animate) {
                startCellSelectionAnimation(prevIndex = oldIndex)
            } else {
                invalidate()
            }
        }
    }

    private fun startCellSelectionAnimation(prevIndex: Int) {
        selectionAnimPrevCellIndex = prevIndex

        selectionAnimator.start()
    }

    private fun onSelectionAnimationTick(fraction: Float) {
        cellStrokeAlpha = (fraction * 255f + 0.5f).toInt()

        invalidate()
    }

    private fun refreshCellStrokeShadowShader() {
        val halfCellSize = cellSize * 0.5f

        // There's nothing special about alpha of cellStrokeColor or stops.
        // It's just hand-picked numbers to get a nice balance between
        // shadow not being too noisy and shadow being noticeable.
        cellStrokeShadowPaint.shader = RadialGradient(
            halfCellSize, halfCellSize,
            halfCellSize - cellStrokeWidth,
            intArrayOf(Color.TRANSPARENT, cellStrokeColor.withAlpha(110)),
            floatArrayOf(0.65f, 1f),
            Shader.TileMode.CLAMP
        )
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.actionMasked) {
            MotionEvent.ACTION_UP -> {
                val x = event.x
                val y = event.y

                if (isPointInCellArea(x, y)) {
                    val cellIndex = getCellByPointOnScreen(x, y)

                    if (cellIndex >= 0) {
                        selectColorAt(cellIndex)
                    }
                }
            }
        }

        return true
    }

    private fun isXInCellArea(x: Float): Boolean {
        return x >= cellAreaHorizontalMargin && x <= width - cellAreaHorizontalMargin
    }

    private fun isYInCellArea(y: Float): Boolean {
        return y >= cellAreaTopMargin && y <= height - cellAreaBottomMargin
    }

    private fun isPointInCellArea(x: Float, y: Float): Boolean {
        return isXInCellArea(x) && isYInCellArea(y)
    }

    /**
     * Calculates cell index using coordinates of point on screen.
     * The point must be in cell area ([isPointInCellArea] should be true).
     * The point can be in cell area but it may not point to a particular cell (points to spacing between cells),
     * so when it's so, the method returns -1.
     */
    private fun getCellByPointOnScreen(x: Float, y: Float): Int {
        val cellSize = cellSize
        val cellSizeWithSpacing = cellSize + cellSpacing
        val hMargin = cellAreaHorizontalMargin
        val topMargin = cellAreaTopMargin

        val column = ((x - hMargin) / cellSizeWithSpacing).toInt()
        val cellLeft = hMargin + column * cellSizeWithSpacing

        // Check whether x coordinate points to the spacing between cells
        if (x > cellLeft + cellSize) {
            return -1
        }

        val row = ((y - topMargin) / cellSizeWithSpacing).toInt()
        val cellTop = topMargin + row * cellSizeWithSpacing

        // Check whether y coordinate points to the spacing between cells
        if (y > cellTop + cellSize) {
            return -1
        }

        return row * cellsInRow + column
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val width = MeasureSpec.getSize(widthMeasureSpec)

        val hMargin = cellAreaHorizontalMargin
        val cellAreaWidth = width.toFloat() - hMargin * 2f
        val cellSizeWithSpacing = cellSize + cellSpacing

        val cellsInRow = (cellAreaWidth / cellSizeWithSpacing).toInt()

        // Ceiling division (colors.size / cellInRow)
        val rows = (colors.size + cellsInRow - 1) / cellsInRow

        val height = (rows * cellSizeWithSpacing + cellAreaTopMargin + cellAreaBottomMargin).toInt()

        setMeasuredDimension(
            widthMeasureSpec,
            resolveSize(height, heightMeasureSpec)
        )
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)

        val hMargin = cellAreaHorizontalMargin
        val cellAreaWidth = w.toFloat() - hMargin * 2f
        val cellSizeWithSpacing = cellSize + cellSpacing

        cellsInRow = (cellAreaWidth / cellSizeWithSpacing).toInt()
        cellRowStart = hMargin + (cellAreaWidth - cellsInRow * cellSizeWithSpacing) * 0.5f
    }

    override fun onDraw(c: Canvas) {
        super.onDraw(c)

        drawOutlineAndTitle(c)
        drawCells(c)
    }

    private fun drawCells(c: Canvas) {
        val colors = colors

        val selAnimIsRunning = selectionAnimator.isRunning
        val selIndex = _selectedIndex
        val selAnimPrevIndex = selectionAnimPrevCellIndex

        var cellLeft = cellRowStart
        var cellTop = cellAreaTopMargin
        val cellSize = cellSize
        val cellSizeWithSpacing = cellSize + cellSpacing

        val fillPaint = cellFillPaint
        val strokePaint = cellStrokePaint

        val cellStrokeWidth = cellStrokeWidth
        val halfCellStrokeWidth = cellStrokeWidth * 0.5f

        var columnIndex = 0

        for (i in 0 until colors.size) {
            val color = colors[i]

            if (columnIndex >= cellsInRow) {
                columnIndex = 0
                cellLeft = cellRowStart
                cellTop += cellSizeWithSpacing
            }

            val cellRight = cellLeft + cellSize
            val cellBottom = cellTop + cellSize

            fillPaint.color = color
            c.drawOval(cellLeft, cellTop, cellRight, cellBottom, fillPaint)

            if (i == selIndex || (selAnimIsRunning && i == selAnimPrevIndex)) {
                val actualAlpha = if (i == selAnimPrevIndex) {
                    255 - cellStrokeAlpha
                } else {
                    cellStrokeAlpha
                }

                strokePaint.alpha = actualAlpha

                c.drawOval(
                    cellLeft + halfCellStrokeWidth,
                    cellTop + halfCellStrokeWidth,
                    cellRight - halfCellStrokeWidth,
                    cellBottom - halfCellStrokeWidth,
                    strokePaint
                )

                if (actualAlpha > 0) {
                    c.withTranslation(cellLeft, cellTop) {
                        if (actualAlpha < 255) {
                            // withTranslation will restore this layer too, so no need store save count and restore it as well.
                            c.saveLayerAlpha(0f, 0f, cellSize, cellSize, actualAlpha)
                        }

                        c.drawOval(
                            cellStrokeWidth,
                            cellStrokeWidth,
                            cellSize - cellStrokeWidth,
                            cellSize - cellStrokeWidth,
                            cellStrokeShadowPaint
                        )
                    }
                }
            }

            cellLeft += cellSizeWithSpacing
            columnIndex++
        }
    }

    private fun drawOutlineAndTitle(c: Canvas) {
        val w = width.toFloat()
        val h = height.toFloat()
        val rr = outlineRoundRadius
        val sw = outlineStrokeWidth
        val th = titleHeight

        val hPadding = titleHorizontalPadding
        val startMargin = titleStartMargin

        c.withSave {
            val clipRight = startMargin + titleWidth + hPadding * 2

            if (Build.VERSION.SDK_INT >= 26) {
                c.clipOutRect(startMargin, 0f, clipRight, th)
            } else {
                @Suppress("DEPRECATION")
                c.clipRect(startMargin, 0f, clipRight, th, Region.Op.DIFFERENCE)
            }

            c.drawRoundRect(sw, th * 0.5f, w - sw, h - sw, rr, rr, outlinePaint)
        }

        c.drawText(title, startMargin + hPadding, th, titlePaint)
    }

    override fun onSaveInstanceState(): Parcelable {
        return SavedState(super.onSaveInstanceState()).also {
            it.selectedIndex = _selectedIndex
        }
    }

    override fun onRestoreInstanceState(state: Parcelable?) {
        if (state is SavedState) {
            val selIndex = state.selectedIndex

            if (selIndex in 0 until colors.size) {
                // The change should be animated because logically nothing is changed, we're just restoring previous state
                selectColorAt(selIndex, animate = false)
            }

            super.onRestoreInstanceState(state.superState)
        }
    }
}