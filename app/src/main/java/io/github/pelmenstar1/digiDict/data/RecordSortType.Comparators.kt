package io.github.pelmenstar1.digiDict.data

import io.github.pelmenstar1.digiDict.common.createComparatorFromField
import io.github.pelmenstar1.digiDict.common.createComparatorFromFieldInverted
import io.github.pelmenstar1.digiDict.common.getEnumFieldCount

private val comparators = arrayOfNulls<Comparator<ConciseRecordWithBadges>>(getEnumFieldCount<RecordSortType>())

private fun createComparator(type: RecordSortType): Comparator<ConciseRecordWithBadges> {
    return when (type) {
        RecordSortType.NEWEST -> createComparatorFromFieldInverted { it.epochSeconds }
        RecordSortType.OLDEST -> createComparatorFromField { it.epochSeconds }
        RecordSortType.GREATEST_SCORE -> createComparatorFromFieldInverted { it.score }
        RecordSortType.LEAST_SCORE -> createComparatorFromField { it.score }
        RecordSortType.ALPHABETIC_BY_EXPRESSION -> createComparatorFromField { it.expression }
        RecordSortType.ALPHABETIC_BY_EXPRESSION_INVERSE -> createComparatorFromFieldInverted { it.expression }
    }
}

fun RecordSortType.getComparatorForConciseRecordWithBadges(): Comparator<ConciseRecordWithBadges> {
    val ordinal = ordinal
    val cachedComparator = comparators[ordinal]

    if (cachedComparator != null) {
        return cachedComparator
    }

    return createComparator(this).also {
        comparators[ordinal] = it
    }
}