package io.github.pelmenstar1.digiDict.common.ui.tests

import android.content.Context
import android.graphics.Color
import android.graphics.Point
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
import kotlin.test.assertFalse

class ColorPaletteViewTests {
    open class TestActivity : AppCompatActivity() {
        lateinit var colorPalette: ColorPaletteView

        override fun onCreate(savedInstanceState: Bundle?) {
            super.onCreate(savedInstanceState)

            setContentView(FrameLayout(this).apply {
                colorPalette = createColorPalette()

                addView(colorPalette)
            })
        }

        open fun createColorPalette(): ColorPaletteView {
            return ColorPaletteView(this)
        }
    }

    class TestActivityForSavedState : TestActivity() {
        override fun createColorPalette(): ColorPaletteView {
            return ColorPaletteView(this).apply {
                id = 101

                addColors(intArrayOf(1, 2, 3))
            }
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

    private fun sendUpEvent(view: ColorPaletteView, pos: PointF) = sendUpEvent(view, pos.x, pos.y)

    private fun sendUpEvent(view: ColorPaletteView, pos: Point) = sendUpEvent(view, pos.x.toFloat(), pos.y.toFloat())

    private fun sendUpEvent(view: ColorPaletteView, posX: Float, posY: Float) {
        val downTime = SystemClock.uptimeMillis()
        val event = MotionEvent.obtain(
            downTime, downTime,
            MotionEvent.ACTION_UP,
            posX, posY,
            0
        )

        view.dispatchTouchEvent(event)
    }

    private fun createColorPalette(colors: IntArray): ColorPaletteView {
        val context = ApplicationProvider.getApplicationContext<Context>()

        return ColorPaletteView(context).apply {
            addColors(colors)
        }
    }

    private fun onMainThread(block: Runnable) {
        InstrumentationRegistry.getInstrumentation().runOnMainSync(block)
    }

    @Test
    fun touchTest() {
        val scenario = launchActivity<TestActivity>()
        var lastSelectedColor = Color.TRANSPARENT

        val colors = IntArray(42) { it + 1 }

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
    fun touchTest_onTouchPositionOutsideCellArea() {
        val scenario = launchActivity<TestActivity>()

        val colors = IntArray(42) { it + 1 }

        scenario.onActivity {
            val palette = it.colorPalette
            var isListenerCalled = false

            palette.onColorSelectedListener = ColorPaletteView.OnColorSelectedListener {
                isListenerCalled = true
            }

            palette.addColors(colors)

            val width = palette.width
            val height = palette.height

            val leftSideStartX = 0
            val leftSideEndX = palette.cellAreaHorizontalMargin.toInt() - 1

            val rightSideStartX = width - palette.cellAreaHorizontalMargin.toInt() + 1
            val rightSideEndX = width

            val topSideStartY = 0
            val topSideEndY = palette.cellAreaTopMargin.toInt() - 1

            val bottomSideStartY = height - palette.cellAreaBottomMargin.toInt() + 1
            val bottomSideEndY = height

            val testPositions = arrayOf(
                // Left side
                Point(leftSideStartX, 0),
                Point(leftSideEndX, 0),
                Point(leftSideStartX, height),
                Point(leftSideEndX, height),
                Point(leftSideStartX, height / 2),
                Point(leftSideEndX, height / 2),

                //Top side
                Point(0, topSideStartY),
                Point(width / 2, topSideEndY),
                Point(width, topSideEndY),

                // Right side
                Point(rightSideStartX, height / 2),
                Point(rightSideEndX, height / 2),
                Point(rightSideStartX, 0),
                Point(rightSideEndX, 0),
                Point(rightSideStartX, height),
                Point(rightSideEndX, height),

                // Bottom side
                Point(0, bottomSideStartY),
                Point(0, bottomSideEndY),
                Point(width, bottomSideStartY),
                Point(width, bottomSideEndY)
            )

            for (pos in testPositions) {
                sendUpEvent(palette, pos)

                assertFalse(isListenerCalled, "the cell was selected: pos = $pos")
            }
        }
    }


    @Test
    fun selectColorOrLastTest() {
        fun testCase(color: Int, expectedSelectedIndex: Int) = onMainThread {
            val view = createColorPalette(colors = intArrayOf(1, 2, 3))

            view.selectColorOrLast(color, animate = false)
            assertEquals(expectedSelectedIndex, view.selectedIndex)
        }

        testCase(color = 1, expectedSelectedIndex = 0)
        testCase(color = 3, expectedSelectedIndex = 2)

        // Color does not exist, so the last cell should be selected
        testCase(color = 4, expectedSelectedIndex = 2)
    }

    @Test
    fun selectColorOrLastTest_noColors() = onMainThread {
        val view = createColorPalette(colors = intArrayOf())

        view.selectColorOrLast(color = 0, animate = false)

        assertEquals(-1, view.selectedIndex)
    }

    @Test
    fun selectLastColorTest() = onMainThread {
        val view = createColorPalette(colors = intArrayOf(1, 2, 3))

        view.selectLastColor(animate = false)

        assertEquals(2, view.selectedIndex)
    }

    @Test
    fun selectLastColorTest_noColors() = onMainThread {
        val view = createColorPalette(colors = intArrayOf())
        view.selectLastColor(animate = false)

        assertEquals(-1, view.selectedIndex)
    }

    @Test
    fun constructorWithAttributesTest() = onMainThread {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val view = LayoutInflater.from(context).inflate(R.layout.test_color_palette, null) as ColorPaletteView

        assertEquals(view.title, "Palette title")
        assertEquals(view.cellStrokeColor, 0x12345678)
        assertContentEquals(intArrayOf(1, 2, 3), view.colors.toArray())
    }

    @Test
    fun savedStateOnSelectedCellTest() {
        val expectedSelectedIndex = 1

        val scenario = launchActivity<TestActivityForSavedState>()
        scenario.onActivity { activity ->
            activity.colorPalette.run {
                selectColorAt(expectedSelectedIndex, animate = false)
            }
        }

        scenario.recreate()

        scenario.onActivity {
            val actualSelectedIndex = it.colorPalette.selectedIndex

            assertEquals(expectedSelectedIndex, actualSelectedIndex)
        }
    }

    @Test
    fun savedStateOnNoSelectedCellTest() {
        val scenario = launchActivity<TestActivityForSavedState>()
        scenario.recreate()

        scenario.onActivity {
            val actualSelectedIndex = it.colorPalette.selectedIndex

            assertEquals(-1, actualSelectedIndex)
        }
    }
}