package io.github.pelmenstar1.digiDict.ui.misc

import android.content.Context
import io.github.pelmenstar1.digiDict.R
import io.github.pelmenstar1.digiDict.common.android.EnumResourcesStringFormatter
import io.github.pelmenstar1.digiDict.data.RecordSortType

class ResourcesRecordSortTypeStringFormatter(
    context: Context
) : EnumResourcesStringFormatter<RecordSortType>(context, RecordSortType::class.java) {
    override fun getResourceId(value: RecordSortType) = when (value) {
        RecordSortType.NEWEST -> R.string.recordSortType_newest
        RecordSortType.OLDEST -> R.string.recordSortType_oldest
        RecordSortType.GREATEST_SCORE -> R.string.recordSortType_greatestScore
        RecordSortType.LEAST_SCORE -> R.string.recordSortType_leastScore
        RecordSortType.ALPHABETIC_BY_EXPRESSION -> R.string.recordSortType_byExpression
        RecordSortType.ALPHABETIC_BY_EXPRESSION_INVERSE -> R.string.recordSortType_byExpressionInverse
    }
}