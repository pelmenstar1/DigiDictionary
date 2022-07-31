package io.github.pelmenstar1.digiDict.prefs

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first

abstract class AppPreferences {
    data class Entry<T : Any>(val defaultValue: T)

    object Entries {
        val scorePointsPerCorrectAnswer = Entry(defaultValue = 1)
        val scorePointsPerWrongAnswer = Entry(defaultValue = 2)
        val useCustomTabs = Entry(defaultValue = true)
    }

    data class Snapshot(
        val scorePointsPerCorrectAnswer: Int,
        val scorePointsPerWrongAnswer: Int,
        val useCustomTabs: Boolean
    ) {
        @Suppress("UNCHECKED_CAST")
        operator fun <T : Any> get(entry: Entry<T>): T {
            return when {
                entry === Entries.scorePointsPerCorrectAnswer -> scorePointsPerCorrectAnswer
                entry === Entries.scorePointsPerWrongAnswer -> scorePointsPerWrongAnswer
                entry === Entries.useCustomTabs -> useCustomTabs
                else -> throw IllegalStateException("Invalid preference entry")
            } as T
        }
    }

    abstract fun getSnapshotFlow(): Flow<Snapshot>
    abstract suspend fun <T : Any> set(entry: Entry<T>, value: T)

    open suspend fun <T : Any> get(entry: Entry<T>): T {
        val snapshot = getSnapshotFlow().first()

        return snapshot[entry]
    }
}

suspend inline fun <T : Any> AppPreferences.get(getEntry: AppPreferences.Entries.() -> AppPreferences.Entry<T>): T {
    return get(AppPreferences.Entries.getEntry())
}