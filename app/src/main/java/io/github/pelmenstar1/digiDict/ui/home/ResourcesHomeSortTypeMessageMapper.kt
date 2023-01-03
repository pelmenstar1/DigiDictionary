package io.github.pelmenstar1.digiDict.ui.home

import android.content.Context
import io.github.pelmenstar1.digiDict.R
import io.github.pelmenstar1.digiDict.common.android.ResourcesMessageMapper
import io.github.pelmenstar1.digiDict.common.getEnumFieldCount
import io.github.pelmenstar1.digiDict.data.HomeSortType

class ResourcesHomeSortTypeMessageMapper(context: Context) : ResourcesMessageMapper<HomeSortType>(
    context, getEnumFieldCount<HomeSortType>()
) {
    override fun mapToStringResource(type: HomeSortType) = when (type) {
        HomeSortType.NEWEST -> R.string.home_sortType_newest
        HomeSortType.OLDEST -> R.string.home_sortType_oldest
        HomeSortType.GREATEST_SCORE -> R.string.home_sortType_greatestScore
        HomeSortType.LEAST_SCORE -> R.string.home_sortType_leastScore
        HomeSortType.ALPHABETIC_BY_EXPRESSION -> R.string.home_sortType_byExpression
        HomeSortType.ALPHABETIC_BY_EXPRESSION_INVERSE -> R.string.home_sortType_byExpressionInverse
    }
}