package io.github.pelmenstar1.digiDict.common

import android.content.Context
import android.graphics.drawable.RippleDrawable
import androidx.appcompat.view.ContextThemeWrapper
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import io.github.pelmenstar1.digiDict.common.android.getColorSurfaceVariant
import io.github.pelmenstar1.digiDict.common.android.getDefaultTextColor
import io.github.pelmenstar1.digiDict.common.android.getPrimaryColor
import io.github.pelmenstar1.digiDict.common.android.getSelectableItemBackground
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.test.assertEquals
import kotlin.test.assertIs

@RunWith(AndroidJUnit4::class)
class ResourcesKtTests {
    private fun getThemeContext(): Context {
        val baseContext = InstrumentationRegistry.getInstrumentation().context

        return ContextThemeWrapper(baseContext, R.style.ResourcesTestTheme)
    }

    @Test
    fun getSelectableItemBackgroundTest() {
        val context = getThemeContext()
        val background = context.getSelectableItemBackground()

        assertIs<RippleDrawable>(background)
    }

    private fun getColorTestHelper(getColor: Context.() -> Int, expected: Long) {
        val context = getThemeContext()
        val actual = context.getColor()

        assertEquals(expected.toInt(), actual)
    }

    @Test
    fun getPrimaryColorTest() = getColorTestHelper({ getPrimaryColor() }, expected = 0xff111111)

    @Test
    fun getDefaultTextColorTest() = getColorTestHelper({ getDefaultTextColor() }, expected = 0xffff0000)

    @Test
    fun getColorSurfaceVariantTest() = getColorTestHelper({ getColorSurfaceVariant() }, expected = 0xff00ff00)
}