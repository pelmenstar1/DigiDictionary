package io.github.pelmenstar1.digiDict.common.time

data class EpochSecondsRange(val start: Long, val endInclusive: Long) {
    init {
        require((start and endInclusive) >= 0 && start <= endInclusive) {
            "Invalid range (start=$start, endInclusive=$endInclusive)"
        }
    }

    companion object {
        val ALL_TIME_SPAN = EpochSecondsRange(0L, Long.MAX_VALUE)
    }
}