package io.github.pelmenstar1.digiDict.utils

import io.github.pelmenstar1.digiDict.common.time.CurrentEpochSecondsProvider

class FakeCurrentEpochSecondsProvider(private val fakeCurrentEpochSeconds: Long) : CurrentEpochSecondsProvider {
    override fun currentEpochSeconds() = fakeCurrentEpochSeconds
}