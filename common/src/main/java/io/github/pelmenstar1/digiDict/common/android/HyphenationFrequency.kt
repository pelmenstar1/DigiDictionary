package io.github.pelmenstar1.digiDict.common.android

import android.os.Build
import android.text.Layout
import androidx.annotation.RequiresApi

/**
 * Defines all possibles variants of hyphenation frequency. It basically redefines `android.text.Layout.HYPHENATION_FREQUENCY_*` values but
 * on enum surface.
 *
 * Android supports customizing hyphenation frequency for text view since API level >= 23, so this class should be used
 * on those API levels too.
 */

enum class HyphenationFrequency(@JvmField val layoutInt: Int) {
    UNSPECIFIED(-1),

    @RequiresApi(23)
    NONE(Layout.HYPHENATION_FREQUENCY_NONE),

    @RequiresApi(23)
    NORMAL(Layout.HYPHENATION_FREQUENCY_NORMAL),

    @RequiresApi(23)
    FULL(Layout.HYPHENATION_FREQUENCY_FULL);

    companion object {
        fun fromOrdinal(ordinal: Int): HyphenationFrequency {
            if (Build.VERSION.SDK_INT >= 23) {
                when (ordinal) {
                    0 -> UNSPECIFIED
                    1 -> NONE
                    2 -> NORMAL
                    3 -> FULL
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