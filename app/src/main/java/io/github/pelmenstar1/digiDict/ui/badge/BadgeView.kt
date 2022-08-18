package io.github.pelmenstar1.digiDict.ui.badge

import android.content.Context
import androidx.core.content.res.ResourcesCompat
import com.google.android.material.textview.MaterialTextView
import io.github.pelmenstar1.digiDict.R

// Instantiated only from the code.
class BadgeView(context: Context) : MaterialTextView(context) {
    init {
        val res = context.resources
        val theme = context.theme

        background = ResourcesCompat.getDrawable(res, R.drawable.badge_view_bg, theme)

        val horizontalPadding = res.getDimensionPixelOffset(R.dimen.badge_paddingHorizontal)
        val verticalPadding = res.getDimensionPixelOffset(R.dimen.badge_paddingVertical)

        setPadding(
            horizontalPadding,
            verticalPadding,
            horizontalPadding,
            verticalPadding
        )
    }
}