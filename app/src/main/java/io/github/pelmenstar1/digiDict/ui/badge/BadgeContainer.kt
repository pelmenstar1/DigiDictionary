package io.github.pelmenstar1.digiDict.ui.badge

import android.content.Context
import android.util.AttributeSet
import androidx.annotation.AttrRes
import androidx.annotation.StyleRes
import io.github.pelmenstar1.digiDict.R
import io.github.pelmenstar1.digiDict.common.ui.MultilineHorizontalLinearLayout
import io.github.pelmenstar1.digiDict.common.ui.adjustViewCount
import io.github.pelmenstar1.digiDict.common.ui.getTypedViewAt
import io.github.pelmenstar1.digiDict.data.RecordBadgeInfo

class BadgeContainer @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    @AttrRes defStyleAttr: Int = 0,
    @StyleRes defStyleRes: Int = 0
) : MultilineHorizontalLinearLayout(context, attrs, defStyleAttr, defStyleRes) {
    private val badgeLayoutParams: MarginLayoutParams

    init {
        val res = context.resources
        val badgeEndMargin = res.getDimensionPixelOffset(R.dimen.badge_endMargin)
        val badgeTopMargin = res.getDimensionPixelOffset(R.dimen.badge_topMargin)

        badgeLayoutParams = MarginLayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT).apply {
            marginEnd = badgeEndMargin
            topMargin = badgeTopMargin
        }
    }

    fun setBadges(values: Array<out RecordBadgeInfo>) {
        val context = context
        adjustViewCount(values.size) {
            addBadgeView(context)
        }

        values.forEachIndexed { index, value ->
            getTypedViewAt<BadgeView>(index).badge = value
        }
    }

    private fun addBadgeView(context: Context) {
        addView(BadgeView(context).apply {
            layoutParams = badgeLayoutParams
        })
    }
}