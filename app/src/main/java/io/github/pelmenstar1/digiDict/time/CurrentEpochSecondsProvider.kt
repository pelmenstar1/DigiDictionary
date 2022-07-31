package io.github.pelmenstar1.digiDict.time

interface CurrentEpochSecondsProvider {
    fun currentEpochSeconds(): Long
}

object SystemEpochSecondsProvider : CurrentEpochSecondsProvider {
    override fun currentEpochSeconds() = System.currentTimeMillis() / 1000
}