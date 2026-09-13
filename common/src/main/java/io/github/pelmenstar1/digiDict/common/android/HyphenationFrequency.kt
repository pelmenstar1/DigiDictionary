package io.github.pelmenstar1.digiDict.common.android

import android.text.Layout

/**
 * Defines all possibles variants of hyphenation frequency. It basically redefines `android.text.Layout.HYPHENATION_FREQUENCY_*` values but
 * on enum surface.
 */
enum class HyphenationFrequency(@JvmField val layoutInt: Int) {
    UNSPECIFIED(-1),
    NONE(Layout.HYPHENATION_FREQUENCY_NONE),
    NORMAL(Layout.HYPHENATION_FREQUENCY_NORMAL),
    FULL(Layout.HYPHENATION_FREQUENCY_FULL);

    companion object {
        fun fromOrdinal(ordinal: Int): HyphenationFrequency = when (ordinal) {
            0 -> UNSPECIFIED
            1 -> NONE
            2 -> NORMAL
            3 -> FULL
            else -> throw IllegalArgumentException("ordinal")
        }
    }
}
