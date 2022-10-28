package io.github.pelmenstar1.digiDict.utils

import io.github.pelmenstar1.digiDict.common.time.CurrentEpochSecondsProvider

class FakeCurrentEpochSecondsProvider(
    private val fakeCurrentEpochSeconds: Long,
    private val timeZoneOffset: Int = 0
) : CurrentEpochSecondsProvider {
    override fun get(locality: CurrentEpochSecondsProvider.Locality): Long {
        var result = fakeCurrentEpochSeconds

        if (locality == CurrentEpochSecondsProvider.Locality.Local) {
            result += timeZoneOffset
        }

        return result
    }
}