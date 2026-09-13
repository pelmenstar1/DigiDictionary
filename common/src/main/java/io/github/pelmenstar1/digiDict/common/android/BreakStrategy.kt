package io.github.pelmenstar1.digiDict.common.android

import android.text.Layout

/**
 * Defines all possibles variants of break strategy. It basically redefines `android.text.Layout.BREAK_STRATEGY_*` values but
 * on enum surface.
 */
enum class BreakStrategy(@JvmField val layoutInt: Int) {
    UNSPECIFIED(-1),
    SIMPLE(Layout.BREAK_STRATEGY_SIMPLE),
    BALANCED(Layout.BREAK_STRATEGY_BALANCED),
    HIGH_QUALITY(Layout.BREAK_STRATEGY_HIGH_QUALITY);

    companion object {
        fun fromOrdinal(ordinal: Int): BreakStrategy = when (ordinal) {
            0 -> UNSPECIFIED
            1 -> SIMPLE
            2 -> BALANCED
            3 -> HIGH_QUALITY
            else -> throw IllegalArgumentException("ordinal")
        }
    }
}
