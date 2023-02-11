package io.github.pelmenstar1.digiDict.ui.misc

import android.content.Context
import io.github.pelmenstar1.digiDict.R
import io.github.pelmenstar1.digiDict.common.android.ResourcesMessageMapper
import io.github.pelmenstar1.digiDict.common.getEnumFieldCount
import io.github.pelmenstar1.digiDict.data.RecordSortType

class ResourcesRecordSortTypeMessageMapper(context: Context) : ResourcesMessageMapper<RecordSortType>(
    context, getEnumFieldCount<RecordSortType>()
) {
    override fun mapToStringResource(type: RecordSortType) = when (type) {
        RecordSortType.NEWEST -> R.string.recordSortType_newest
        RecordSortType.OLDEST -> R.string.recordSortType_oldest
        RecordSortType.GREATEST_SCORE -> R.string.recordSortType_greatestScore
        RecordSortType.LEAST_SCORE -> R.string.recordSortType_leastScore
        RecordSortType.ALPHABETIC_BY_EXPRESSION -> R.string.recordSortType_byExpression
        RecordSortType.ALPHABETIC_BY_EXPRESSION_INVERSE -> R.string.recordSortType_byExpressionInverse
    }
}