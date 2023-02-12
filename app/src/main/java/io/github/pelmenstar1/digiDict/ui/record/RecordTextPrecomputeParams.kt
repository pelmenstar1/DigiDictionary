package io.github.pelmenstar1.digiDict.ui.record

import android.text.PrecomputedText
import androidx.annotation.RequiresApi

/**
 * Stores the [PrecomputedText.Params] for each supported property of a record that supports precomputing.
 *
 * The class is available since API level 28, as there's no way to create [PrecomputedText.Params] on lower API levels.
 */
@RequiresApi(28)
data class RecordTextPrecomputeParams(
    val expressionParams: PrecomputedText.Params,
    val meaningParams: PrecomputedText.Params
)