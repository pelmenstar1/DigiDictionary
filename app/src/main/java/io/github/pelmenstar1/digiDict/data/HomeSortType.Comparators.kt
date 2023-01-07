package io.github.pelmenstar1.digiDict.data

import io.github.pelmenstar1.digiDict.common.createComparatorFromField
import io.github.pelmenstar1.digiDict.common.createComparatorFromFieldInverted
import io.github.pelmenstar1.digiDict.common.getLazyValue

private var newestComparator: Comparator<ConciseRecordWithBadges>? = null
private var oldestComparator: Comparator<ConciseRecordWithBadges>? = null
private var greatestScoreComparator: Comparator<ConciseRecordWithBadges>? = null
private var leastScoreComparator: Comparator<ConciseRecordWithBadges>? = null
private var alphabeticByExprComparator: Comparator<ConciseRecordWithBadges>? = null
private var alphabeticByExprInverseComparator: Comparator<ConciseRecordWithBadges>? = null

fun HomeSortType.getComparatorForConciseRecordWithBadges(): Comparator<ConciseRecordWithBadges> {
    return when (this) {
        HomeSortType.NEWEST -> getLazyValue(
            newestComparator,
            { createComparatorFromFieldInverted { it.epochSeconds } },
            { newestComparator = it }
        )

        HomeSortType.OLDEST -> getLazyValue(
            oldestComparator,
            { createComparatorFromField { it.epochSeconds } },
            { oldestComparator = it }
        )

        HomeSortType.GREATEST_SCORE -> getLazyValue(
            greatestScoreComparator,
            { createComparatorFromFieldInverted { it.score } },
            { greatestScoreComparator = it }
        )

        HomeSortType.LEAST_SCORE -> getLazyValue(
            leastScoreComparator,
            { createComparatorFromField { it.score } },
            { leastScoreComparator = it }
        )

        HomeSortType.ALPHABETIC_BY_EXPRESSION -> getLazyValue(
            alphabeticByExprComparator,
            { createComparatorFromField { it.expression } },
            { alphabeticByExprComparator = it }
        )

        HomeSortType.ALPHABETIC_BY_EXPRESSION_INVERSE -> getLazyValue(
            alphabeticByExprInverseComparator,
            { createComparatorFromFieldInverted { it.expression } },
            { alphabeticByExprInverseComparator = it }
        )
    }
}