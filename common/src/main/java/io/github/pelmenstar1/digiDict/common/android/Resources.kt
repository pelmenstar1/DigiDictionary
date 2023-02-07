package io.github.pelmenstar1.digiDict.common.android

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.Drawable
import android.util.TypedValue
import androidx.annotation.ColorInt
import androidx.core.content.res.ResourcesCompat

fun Context.getSelectableItemBackground(): Drawable? {
    val theme = theme
    val typedValue = TypedValue()

    val isResolved =
        theme.resolveAttribute(com.google.android.material.R.attr.selectableItemBackground, typedValue, true)

    return if (isResolved) {
        ResourcesCompat.getDrawable(resources, typedValue.resourceId, theme)
    } else {
        null
    }
}

@ColorInt
fun Context.getPrimaryColor(): Int {
    val typedValue = TypedValue()
    val isResolved = theme.resolveAttribute(com.google.android.material.R.attr.colorPrimary, typedValue, true)

    return if (isResolved) {
        typedValue.data
    } else {
        Color.TRANSPARENT
    }
}

@ColorInt
fun Context.getDefaultTextColor(): Int {
    val typedValue = TypedValue()
    val isResolved = theme.resolveAttribute(android.R.attr.textColor, typedValue, true)

    return if (isResolved) {
        typedValue.data
    } else {
        Color.BLACK
    }
}