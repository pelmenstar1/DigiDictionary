package io.github.pelmenstar1.digiDict.utils

import io.github.pelmenstar1.digiDict.common.equalsPattern

data class IntRangeSection(val ranges: Array<out IntRange>) {
    override fun equals(other: Any?) = equalsPattern(other) { o ->
        ranges.contentEquals(o.ranges)
    }

    override fun hashCode() = ranges.contentHashCode()
}

fun IntRangeSection(vararg ranges: IntRange): IntRangeSection = IntRangeSection(ranges)