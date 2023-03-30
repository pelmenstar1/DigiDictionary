package io.github.pelmenstar1.digiDict.common.ui.tests

import android.content.Context
import android.graphics.Color
import android.graphics.PointF
import android.os.Bundle
import android.os.SystemClock
import android.view.LayoutInflater
import android.view.MotionEvent
import android.widget.FrameLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.test.core.app.ApplicationProvider
import androidx.test.platform.app.InstrumentationRegistry
import io.github.pelmenstar1.digiDict.common.ui.ColorPaletteView
import io.github.pelmenstar1.digiDict.commonTestUtils.launchActivity
import org.junit.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals

class ColorPaletteViewTests {
    class TestActivity : AppCompatActivity() {
        lateinit var colorPalette: ColorPaletteView

        override fun onCreate(savedInstanceState: Bundle?) {
            super.onCreate(savedInstanceState)

            setContentView(FrameLayout(this).apply {
                colorPalette = ColorPaletteView(context)

                addView(colorPalette)
            })
        }
    }

    private fun calculateCellPosition(view: ColorPaletteView, cellIndex: Int): PointF {
        val baseTop = view.cellAreaTopMargin
        val baseLeft = view.cellAreaHorizontalMargin
        val cellSize = view.cellSize
        val cellSpacing = view.cellSpacing

        val rowIndex = cellIndex / view.cellsInRow
        val columnIndex = cellIndex % view.cellsInRow

        val x = baseLeft + columnIndex * (cellSize + cellSpacing)
        val y = baseTop + rowIndex * (cellSize + cellSpacing)

        return PointF(x, y)
    }

    private fun sendUpEvent(view: ColorPaletteView, pos: PointF) {
        val downTime = SystemClock.uptimeMillis()
        val event = MotionEvent.obtain(
            downTime, downTime,
            MotionEvent.ACTION_UP,
            pos.x, pos.y,
            0
        )

        view.dispatchTouchEvent(event)
    }

    @Test
    fun onColorSelectedTest() {
        val scenario = launchActivity<TestActivity>()
        var lastSelectedColor = Color.TRANSPARENT

        val colors = IntArray(42) { i ->
            i + 1
        }

        scenario.onActivity {
            val palette = it.colorPalette

            palette.onColorSelectedListener = ColorPaletteView.OnColorSelectedListener { color ->
                lastSelectedColor = color
            }

            palette.addColors(colors)

            for ((i, color) in colors.withIndex()) {
                val viewPos = calculateCellPosition(palette, i)
                sendUpEvent(palette, viewPos)

                assertEquals(color, lastSelectedColor)
            }
        }
    }

    @Test
    fun constructorWithAttributeTest() {
        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            val context = ApplicationProvider.getApplicationContext<Context>()
            val view = LayoutInflater.from(context).inflate(R.layout.test_color_palette, null) as ColorPaletteView

            assertEquals(view.title, "Palette title")
            assertEquals(view.cellStrokeColor, 0x12345678)
            assertContentEquals(intArrayOf(1, 2, 3), view.colors.toArray())
        }
    }
}