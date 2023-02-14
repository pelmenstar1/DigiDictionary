package io.github.pelmenstar1.digiDict.common.android

import android.os.Build
import android.text.Layout
import androidx.annotation.RequiresApi

/**
 * Defines all possibles variants of break strategy. It basically redefines `android.text.Layout.BREAK_STRATEGY_*` values but
 * on enum surface.
 *
 * Android supports customizing break strategy for text view since API level >= 23, so this class should be used
 * on those API levels too.
 */
enum class BreakStrategy(@JvmField val layoutInt: Int) {
    UNSPECIFIED(-1),

    @RequiresApi(23)
    SIMPLE(Layout.BREAK_STRATEGY_SIMPLE),

    @RequiresApi(23)
    BALANCED(Layout.BREAK_STRATEGY_BALANCED),

    @RequiresApi(23)
    HIGH_QUALITY(Layout.BREAK_STRATEGY_HIGH_QUALITY);

    companion object {
        fun fromOrdinal(ordinal: Int): BreakStrategy {
            if (Build.VERSION.SDK_INT >= 23) {
                when (ordinal) {
                    0 -> return UNSPECIFIED
                    1 -> return SIMPLE
                    2 -> return BALANCED
                    3 -> return HIGH_QUALITY
                }
            } else {
                if (ordinal == 0) {
                    return UNSPECIFIED
                }
            }

            throw IllegalArgumentException("ordinal")
        }
    }
}