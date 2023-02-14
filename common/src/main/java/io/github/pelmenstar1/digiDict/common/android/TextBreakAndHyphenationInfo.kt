package io.github.pelmenstar1.digiDict.common.android

/**
 * Stores the information about break strategy and hyphenation.
 *
 * Android supports these values since API level >= 23,
 * so if the API level is lower, both [breakStrategy] and [hyphenationFrequency] are expected to be unspecified
 */
data class TextBreakAndHyphenationInfo(
    val breakStrategy: BreakStrategy,
    val hyphenationFrequency: HyphenationFrequency
) {
    companion object {
        /**
         * The [TextBreakAndHyphenationInfo] instance whose [breakStrategy] and [hyphenationFrequency] are `-1`.
         */
        val UNSPECIFIED = TextBreakAndHyphenationInfo(BreakStrategy.UNSPECIFIED, HyphenationFrequency.UNSPECIFIED)
    }
}