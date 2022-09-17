package io.github.pelmenstar1.digiDict.common

import android.content.Context
import android.graphics.drawable.Drawable
import android.util.TypedValue
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