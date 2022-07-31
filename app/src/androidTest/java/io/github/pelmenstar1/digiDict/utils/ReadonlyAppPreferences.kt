package io.github.pelmenstar1.digiDict.utils

import io.github.pelmenstar1.digiDict.prefs.AppPreferences
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

class ReadonlyAppPreferences(private val snapshot: Snapshot) : AppPreferences() {
    override fun getSnapshotFlow(): Flow<Snapshot> = flowOf(snapshot)

    override suspend fun <T : Any> set(entry: Entry<T>, value: T) {
        throw NotImplementedError()
    }
}