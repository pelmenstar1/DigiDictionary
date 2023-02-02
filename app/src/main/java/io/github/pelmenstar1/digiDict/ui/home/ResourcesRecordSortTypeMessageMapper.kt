package io.github.pelmenstar1.digiDict.ui.home

import android.content.Context
import io.github.pelmenstar1.digiDict.R
import io.github.pelmenstar1.digiDict.common.android.ResourcesMessageMapper
import io.github.pelmenstar1.digiDict.common.getEnumFieldCount
import io.github.pelmenstar1.digiDict.data.RecordSortType

class ResourcesRecordSortTypeMessageMapper(context: Context) : ResourcesMessageMapper<RecordSortType>(
    context, getEnumFieldCount<RecordSortType>()
) {
    override fun mapToStringResource(type: RecordSortType) = when (type) {
        RecordSortType.NEWEST -> R.string.home_sortType_newest
        RecordSortType.OLDEST -> R.string.home_sortType_oldest
        RecordSortType.GREATEST_SCORE -> R.string.home_sortType_greatestScore
        RecordSortType.LEAST_SCORE -> R.string.home_sortType_leastScore
        RecordSortType.ALPHABETIC_BY_EXPRESSION -> R.string.home_sortType_byExpression
        RecordSortType.ALPHABETIC_BY_EXPRESSION_INVERSE -> R.string.home_sortType_byExpressionInverse
    }
}