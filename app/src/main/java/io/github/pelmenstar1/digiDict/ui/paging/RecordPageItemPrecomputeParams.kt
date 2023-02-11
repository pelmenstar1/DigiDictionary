package io.github.pelmenstar1.digiDict.ui.paging

import android.text.PrecomputedText
import androidx.annotation.RequiresApi

@RequiresApi(29)
data class RecordPageItemPrecomputeParams(
    val expressionParams: PrecomputedText.Params,
    val meaningParams: PrecomputedText.Params
)