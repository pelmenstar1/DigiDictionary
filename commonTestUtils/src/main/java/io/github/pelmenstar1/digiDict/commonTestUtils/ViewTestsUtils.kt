package io.github.pelmenstar1.digiDict.commonTestUtils

import android.view.View
import android.view.ViewGroup
import androidx.core.view.children

inline fun <reified T : View> ViewGroup.firstViewOfType() = firstViewOfType(T::class.java)

@Suppress("UNCHECKED_CAST")
fun <T : View> ViewGroup.firstViewOfType(c: Class<T>): T? {
    return children.firstOrNull { c.isInstance(it) } as T?
}