package io.github.pelmenstar1.digiDict.common.android

import android.text.Layout

/**
 * Stores the information about break strategy and hyphenation.
 *
 * Android supports these values since API level >= 23,
 * so if the API level is lower, both [breakStrategy] and [hyphenationFrequency] are expected to be `-1`.
 */
data class TextBreakAndHyphenationInfo(
    /**
     * It should be one of:
     * - `-1` if the API level < 23 or when the break strategy is unspecified.
     * - [Layout.BREAK_STRATEGY_SIMPLE]
     * - [Layout.BREAK_STRATEGY_BALANCED]
     * - [Layout.BREAK_STRATEGY_HIGH_QUALITY]
     */
    val breakStrategy: Int,

    /**
     * It should be one of:
     * - `-1` if the API level < 23 or when the break strategy is unspecified.
     * - [Layout.HYPHENATION_FREQUENCY_NONE]
     * - [Layout.HYPHENATION_FREQUENCY_NORMAL]
     * - [Layout.HYPHENATION_FREQUENCY_FULL]
     */
    val hyphenationFrequency: Int
) {
    companion object {
        /**
         * The [TextBreakAndHyphenationInfo] instance whose [breakStrategy] and [hyphenationFrequency] are `-1`.
         */
        val UNSPECIFIED = TextBreakAndHyphenationInfo(-1, -1)
    }
}