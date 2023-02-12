package io.github.pelmenstar1.digiDict.ui.record

import android.text.PrecomputedText
import android.text.TextPaint
import androidx.annotation.RequiresApi
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
    constructor(expressionPaint: TextPaint, meaningPaint: TextPaint, info: TextBreakAndHyphenationInfo) :
            this(createPrecomputedTextParams(expressionPaint, info), createPrecomputedTextParams(meaningPaint, info))

    companion object {
        internal fun createPrecomputedTextParams(
            paint: TextPaint,
            info: TextBreakAndHyphenationInfo
        ): PrecomputedText.Params {
            return PrecomputedText.Params.Builder(paint)
                .setBreakStrategy(info.breakStrategy)
                .setHyphenationFrequency(info.hyphenationFrequency)
                .build()
        }
    }
}