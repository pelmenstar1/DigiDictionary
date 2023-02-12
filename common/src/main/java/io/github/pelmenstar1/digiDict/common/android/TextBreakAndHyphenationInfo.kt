package io.github.pelmenstar1.digiDict.common.android

import android.text.Layout
import androidx.annotation.RequiresApi

/**
 * Stores the information about break strategy and hyphenation.
 *
 * As customizing of break strategy and hyphenation frequency is available since API level 23,
 * the class should be used only on API level >= 23 as well.
 */
@RequiresApi(23)
data class TextBreakAndHyphenationInfo(
    /**
     * It should be one of:
     * - [Layout.BREAK_STRATEGY_SIMPLE]
     * - [Layout.BREAK_STRATEGY_BALANCED]
     * - [Layout.BREAK_STRATEGY_HIGH_QUALITY]
     */
    val breakStrategy: Int,

    /**
     * It should be one of:
     * - [Layout.HYPHENATION_FREQUENCY_NONE]
     * - [Layout.HYPHENATION_FREQUENCY_NORMAL]
     * - [Layout.HYPHENATION_FREQUENCY_FULL]
     */
    val hyphenationFrequency: Int
)