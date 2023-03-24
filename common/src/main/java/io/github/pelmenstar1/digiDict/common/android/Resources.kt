package io.github.pelmenstar1.digiDict.common.android

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.Drawable
import android.util.TypedValue
import androidx.annotation.AttrRes
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
private fun Context.getColorFromAttribute(@AttrRes attrId: Int, @ColorInt defaultColor: Int): Int {
    val typedValue = TypedValue()
    val isResolved = theme.resolveAttribute(attrId, typedValue, true)

    if (isResolved && typedValue.type in TypedValue.TYPE_FIRST_COLOR_INT..TypedValue.TYPE_LAST_COLOR_INT) {
        return typedValue.data
    }

    return defaultColor
}

@ColorInt
fun Context.getPrimaryColor(@ColorInt defaultColor: Int = Color.TRANSPARENT): Int {
    return getColorFromAttribute(com.google.android.material.R.attr.colorPrimary, defaultColor)
}

@ColorInt
fun Context.getDefaultTextColor(@ColorInt defaultColor: Int = Color.BLACK): Int {
    return getColorFromAttribute(android.R.attr.textColor, defaultColor)
}

@ColorInt
fun Context.getColorSurfaceVariant(@ColorInt defaultColor: Int = Color.TRANSPARENT): Int {
    return getColorFromAttribute(com.google.android.material.R.attr.colorSurfaceVariant, defaultColor)
}