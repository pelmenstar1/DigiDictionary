package io.github.pelmenstar1.digiDict.ui.badge

import android.content.Context
import com.google.android.material.textview.MaterialTextView
import io.github.pelmenstar1.digiDict.R
import io.github.pelmenstar1.digiDict.common.android.MaxRoundRectDrawable
import io.github.pelmenstar1.digiDict.data.RecordBadgeInfo

// Instantiated only from the code.
class BadgeView(context: Context) : MaterialTextView(context) {
    private val badgeBackground: MaxRoundRectDrawable

    private var _badge: RecordBadgeInfo? = null
    var badge: RecordBadgeInfo
        get() = requireNotNull(_badge)
        set(value) {
            if (_badge != value) {
                _badge = value

                text = value.name
                badgeBackground.color = value.outlineColor
            }
        }

    init {
        val res = context.resources

        val horizontalPadding = res.getDimensionPixelOffset(R.dimen.badge_paddingHorizontal)
        val verticalPadding = res.getDimensionPixelOffset(R.dimen.badge_paddingVertical)

        setPadding(horizontalPadding, verticalPadding, horizontalPadding, verticalPadding)

        badgeBackground = BadgeOutlineHelper.createBackground(context).also {
            background = it
        }

        ellipsize = null
    }
}