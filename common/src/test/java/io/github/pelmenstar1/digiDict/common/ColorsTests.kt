package io.github.pelmenstar1.digiDict.common

import android.graphics.Color
import androidx.annotation.ColorInt
import androidx.core.graphics.alpha
import androidx.core.graphics.blue
import androidx.core.graphics.green
import androidx.core.graphics.red
import kotlin.test.Test
import kotlin.test.assertEquals

class ColorsTests {
    @Test
    fun withAlphaTest() {
        fun testCase(@ColorInt color: Int, newAlpha: Int) {
            val colorWithNewAlpha = color.withAlpha(newAlpha)

            assertEquals(colorWithNewAlpha.alpha, newAlpha)
            assertEquals(color.red, colorWithNewAlpha.red)
            assertEquals(color.green, colorWithNewAlpha.green)
            assertEquals(color.blue, colorWithNewAlpha.blue)
        }

        testCase(0x25FF00FF, 60)
        testCase(0, 60)
        testCase(0xFFFFFFFF.toInt(), 0)
    }

    @Test
    fun transparentTest() {
        fun testCase(@ColorInt color: Int) {
            val transparentColor = color.transparent()

            assertEquals(0, transparentColor.alpha)
            assertEquals(color.red, transparentColor.red)
            assertEquals(color.green, transparentColor.green)
            assertEquals(color.blue, transparentColor.blue)
        }

        testCase(Color.WHITE)
        testCase(Color.BLACK)
        testCase(Color.TRANSPARENT)
    }
}