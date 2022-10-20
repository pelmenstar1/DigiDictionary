package io.github.pelmenstar1.digiDict.common.time

import java.util.*

interface CurrentEpochSecondsProvider {
    class Locality private constructor() {
        companion object {
            val Utc = Locality()
            val Local = Locality()
        }
    }

    fun get(locality: Locality): Long
}

inline fun CurrentEpochSecondsProvider.get(
    selector: CurrentEpochSecondsProvider.Locality.Companion.() -> CurrentEpochSecondsProvider.Locality
): Long {
    return get(CurrentEpochSecondsProvider.Locality.selector())
}

object SystemEpochSecondsProvider : CurrentEpochSecondsProvider {
    override fun get(locality: CurrentEpochSecondsProvider.Locality): Long {
        var resultMillis = System.currentTimeMillis()

        if (locality == CurrentEpochSecondsProvider.Locality.Local) {
            resultMillis += TimeZone.getDefault().getOffset(resultMillis)
        }

        return resultMillis / 1000
    }
}