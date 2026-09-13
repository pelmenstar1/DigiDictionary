package io.github.pelmenstar1.digiDict.commonTestUtils

import android.view.View
import android.view.ViewGroup
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.children
import androidx.core.view.updatePadding

inline fun <reified T : View> ViewGroup.firstViewOfType() = firstViewOfType(T::class.java)

@Suppress("UNCHECKED_CAST")
fun <T : View> ViewGroup.firstViewOfType(c: Class<T>): T? {
    return children.firstOrNull { c.isInstance(it) } as T?
}

/**
 * Pads the receiver by the system bar insets.
 *
 * From API 35 on, an activity always draws edge-to-edge, so the content view of a bare test
 * activity starts underneath the status bar. A view placed at the top of such an activity then
 * overlaps the status bar, and an Espresso click on it is delivered to the system rather than to
 * the view. Test activities that host their content directly should call this, which is the
 * equivalent of what MainActivity does for the real screens.
 */
fun View.padBySystemBarInsets() {
    ViewCompat.setOnApplyWindowInsetsListener(this) { view, windowInsets ->
        val bars = windowInsets.getInsets(
            WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.displayCutout()
        )

        view.updatePadding(left = bars.left, top = bars.top, right = bars.right, bottom = bars.bottom)

        windowInsets
    }
}
