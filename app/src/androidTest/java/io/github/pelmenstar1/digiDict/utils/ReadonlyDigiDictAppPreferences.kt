package io.github.pelmenstar1.digiDict.utils

import io.github.pelmenstar1.digiDict.prefs.DigiDictAppPreferences
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

class ReadonlyDigiDictAppPreferences(private val snapshot: Snapshot) : DigiDictAppPreferences() {
    override fun getSnapshotFlow(): Flow<Snapshot> = flowOf(snapshot)

    override suspend fun <T : Any> set(entry: Entry<T, Entries>, value: T) {
        throw NotImplementedError()
    }
}