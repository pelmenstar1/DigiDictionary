package io.github.pelmenstar1.digiDict.common.ui

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.os.Parcel
import android.os.Parcelable
import android.util.AttributeSet
import android.view.AbsSavedState
import android.view.View
import android.view.View.OnClickListener
import androidx.annotation.AttrRes
import androidx.annotation.ColorInt
import androidx.annotation.StyleRes
import io.github.pelmenstar1.digiDict.common.EmptyArray
import io.github.pelmenstar1.digiDict.common.withAddedElements
import java.util.*

class ColorPaletteView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    @AttrRes defStyleAttr: Int = 0,
    @StyleRes defStyleRes: Int = 0
) : MultilineHorizontalLinearLayout(context, attrs, defStyleAttr, defStyleRes) {
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

    private class CellView(context: Context) : View(context) {
        private val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
        }

        private val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
        }

        var fillColor: Int
            get() = fillPaint.color
            set(value) {
                fillPaint.color = value
                invalidate()
            }

        var strokeColor: Int
            get() = strokePaint.color
            set(value) {
                strokePaint.color = value
                invalidate()
            }

        var strokeWidth: Float
            get() = strokePaint.strokeWidth
            set(value) {
                strokePaint.strokeWidth = value
                invalidate()
            }

        var isCellSelected: Boolean = false
            set(value) {
                field = value
                invalidate()
            }

        override fun onDraw(canvas: Canvas) {
            val w = width.toFloat()
            val h = height.toFloat()

            val sw = strokeWidth
            val hsw = sw * 0.5f
            val dsw = sw * 2f

            canvas.drawOval(dsw, dsw, w - dsw, h - dsw, fillPaint)
            if (isCellSelected) {
                canvas.drawOval(hsw, hsw, w - hsw, h - hsw, strokePaint)
            }
        }
    }

    private val cellOnClickListener = OnClickListener {
        selectedIndex = it.tag as Int
    }

    private val cellLayoutParams: LayoutParams
    private val cellRoundRadii: FloatArray
    private val cellStrokeWidth: Float

    private var colors = EmptyArray.INT

    val selectedColor: Int
        get() = colors[selectedIndex]

    var selectedIndex = -1
        set(value) {
            selectedIndex.let {
                if (it >= 0) {
                    setCellSelectedStateAt(it, false)
                }
            }

            field = value

            setCellSelectedStateAt(value, true)
        }

    var cellStrokeColor = 0
        set(value) {
            field = value

            for (i in 0 until childCount) {
                getTypedViewAt<CellView>(i).strokeColor = value
            }
        }

    init {
        with(context.resources) {
            getDimension(R.dimen.colorPalette_cellRoundRadius).also { radius ->
                cellRoundRadii = FloatArray(8).also { radii ->
                    Arrays.fill(radii, radius)
                }
            }

            val spacing = getDimensionPixelOffset(R.dimen.colorPalette_spacing)
            getDimensionPixelSize(R.dimen.colorPalette_cellSize).also { size ->
                cellLayoutParams = MarginLayoutParams(size, size).also {
                    it.marginEnd = spacing
                    it.bottomMargin = spacing
                }
            }

            cellStrokeWidth = getDimension(R.dimen.colorPalette_strokeWidth)
        }

        attrs?.let {
            val a = context.obtainStyledAttributes(it, R.styleable.ColorPaletteView, defStyleAttr, defStyleRes)

            try {
                if (a.hasValue(R.styleable.ColorPaletteView_colors)) {
                    a.getResourceId(R.styleable.ColorPaletteView_colors, 0).also { colorsRes ->
                        addColors(context.resources.getIntArray(colorsRes))
                    }
                }

                cellStrokeColor = a.getColor(R.styleable.ColorPaletteView_strokeColor, 0)
            } finally {
                a.recycle()
            }
        }
    }

    /**
     * Adds multiple colors to the palette. It's not checked, but all colors in the palette are expected to be unique.
     */
    fun addColors(values: IntArray) {
        colors = colors.withAddedElements(values)
        values.forEach(::addColorInternal)
    }

    private fun addColorInternal(@ColorInt color: Int) {
        val index = childCount

        addView(createCellView(color, index))
    }

    private fun createCellView(@ColorInt color: Int, index: Int): View {
        return CellView(context).apply {
            layoutParams = cellLayoutParams

            fillColor = color
            strokeWidth = cellStrokeWidth
            strokeColor = cellStrokeColor
            tag = index

            setOnClickListener(cellOnClickListener)
        }
    }

    /**
     * Selects specified [color] in the palette. Returns true if given color is actually found in the palette, otherwise false.
     */
    fun selectColor(@ColorInt color: Int): Boolean {
        return colors.indexOf(color).let {
            val found = it >= 0
            if (found) {
                selectedIndex = it
            }

            found
        }
    }

    fun selectLastColor() {
        val size = colors.size

        if (size > 0) {
            selectedIndex = size - 1
        }
    }

    private fun setCellSelectedStateAt(index: Int, value: Boolean) {
        getTypedViewAt<CellView>(index).isCellSelected = value
    }

    override fun onSaveInstanceState(): Parcelable {
        return SavedState(super.onSaveInstanceState()).also {
            it.selectedIndex = selectedIndex
        }
    }

    override fun onRestoreInstanceState(state: Parcelable?) {
        if (state is SavedState) {
            selectedIndex = state.selectedIndex

            super.onRestoreInstanceState(state.superState)
        }
    }
}