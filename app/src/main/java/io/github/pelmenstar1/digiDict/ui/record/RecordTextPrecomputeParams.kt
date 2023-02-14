package io.github.pelmenstar1.digiDict.ui.record

import android.text.PrecomputedText
import android.text.TextPaint
import androidx.annotation.RequiresApi
import io.github.pelmenstar1.digiDict.common.android.BreakStrategy
import io.github.pelmenstar1.digiDict.common.android.HyphenationFrequency
import io.github.pelmenstar1.digiDict.common.android.TextBreakAndHyphenationInfo

/**
 * Stores the [PrecomputedText.Params] for each supported property of a record that supports precomputing.
 *
 * The class is available since API level 28, as there's no way to create [PrecomputedText.Params] on lower API levels.
 */
@RequiresApi(28)
data class RecordTextPrecomputeParams(
    val expressionParams: PrecomputedText.Params,
    val meaningParams: PrecomputedText.Params
) {
    /**
     * Constructs [RecordTextPrecomputeParams] instance from [TextPaint]s for expression and meaning and
     * info about break strategy and hyphenation frequency.
     *
     * The [info] value is expected to be valid that break strategy and hyphenation frequency must not
     * be `-1`.
     */
    constructor(expressionPaint: TextPaint, meaningPaint: TextPaint, info: TextBreakAndHyphenationInfo) :
            this(createPrecomputedTextParams(expressionPaint, info), createPrecomputedTextParams(meaningPaint, info))

    companion object {
        internal fun createPrecomputedTextParams(
            paint: TextPaint,
            info: TextBreakAndHyphenationInfo
        ): PrecomputedText.Params {
            val breakStrategy = info.breakStrategy
            val hyphenationFreq = info.hyphenationFrequency

            if (breakStrategy == BreakStrategy.UNSPECIFIED || hyphenationFreq == HyphenationFrequency.UNSPECIFIED) {
                throw IllegalArgumentException("info should not be unspecified")
            }

            return PrecomputedText.Params.Builder(paint)
                .setBreakStrategy(breakStrategy.layoutInt)
                .setHyphenationFrequency(hyphenationFreq.layoutInt)
                .build()
        }
    }
}