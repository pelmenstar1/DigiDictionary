package io.github.pelmenstar1.digiDict.ui.record

import android.text.PrecomputedText

/**
 * Stores precomputed text values of a record.
 *
 * The class doesn't implement [equals] and [hashCode] methods as
 * there's no support of it in [PrecomputedText].
 */
class RecordTextPrecomputedValues(
    val expression: PrecomputedText,
    val meaning: PrecomputedText
) {
    override fun toString(): String {
        return "RecordTextPrecomputedValues(expression=$expression, meaning=$meaning)"
    }
}