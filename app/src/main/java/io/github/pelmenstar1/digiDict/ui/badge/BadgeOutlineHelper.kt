package io.github.pelmenstar1.digiDict.ui.badge

import android.content.Context
import android.graphics.Paint
import io.github.pelmenstar1.digiDict.R
import io.github.pelmenstar1.digiDict.common.android.MaxRoundRectDrawable

object BadgeOutlineHelper {
    fun createBackground(context: Context): MaxRoundRectDrawable {
        return MaxRoundRectDrawable().apply {
            style = Paint.Style.STROKE
            strokeWidth = context.resources.getDimension(R.dimen.badge_strokeWidth)
        }
    }
}