package io.github.pelmenstar1.digiDict.ui.badge

import android.content.Context
import android.util.AttributeSet
import androidx.annotation.AttrRes
import androidx.annotation.StyleRes
import io.github.pelmenstar1.digiDict.R
import io.github.pelmenstar1.digiDict.common.ui.MultilineHorizontalLinearLayout
import io.github.pelmenstar1.digiDict.common.ui.adjustViewCount
import io.github.pelmenstar1.digiDict.common.ui.getTypedViewAt
import io.github.pelmenstar1.digiDict.data.RecordBadgeNameUtil

class BadgeContainer @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    @AttrRes defStyleAttr: Int = 0,
    @StyleRes defStyleRes: Int = 0
) : MultilineHorizontalLinearLayout(context, attrs, defStyleAttr, defStyleRes) {
    private val badgeLayoutParams: MarginLayoutParams

    init {
        //useMarginLeftOnRowFirstItem = false

        val res = context.resources
        val badgeEndMargin = res.getDimensionPixelOffset(R.dimen.badge_endMargin)
        val badgeTopMargin = res.getDimensionPixelOffset(R.dimen.badge_topMargin)

        badgeLayoutParams = MarginLayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT).apply {
            marginEnd = badgeEndMargin
            topMargin = badgeTopMargin
        }
    }

    fun setBadges(rawBadges: String) {
        setBadges(RecordBadgeNameUtil.decodeArray(rawBadges))
    }

    fun setBadges(values: Array<out String>) {
        val ctx = context
        adjustViewCount(values.size) {
            addView(BadgeView(ctx).apply {
                layoutParams = badgeLayoutParams
            })
        }

        values.forEachIndexed { index, name ->
            getTypedViewAt<BadgeView>(index).text = name
        }
    }
}