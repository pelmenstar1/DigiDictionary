package io.github.pelmenstar1.digiDict.common.preferences

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

abstract class DataStoreAppPreferences<TEntries : AppPreferences.Entries, TSnapshot : AppPreferences.Snapshot<TEntries>>(
    private val dataStore: DataStore<Preferences>
) : AppPreferences<TEntries, TSnapshot>() {
    private val snapshotFlow = dataStore.data.map { it.toSnapshot() }

    override fun getSnapshotFlow() = snapshotFlow

    override suspend fun <TValue : Any> get(entry: Entry<TValue, TEntries>): TValue {
        val prefs = dataStore.data.first()

        return prefs.getValue(entry.getKey(), entry)
    }

    override fun <TValue : Any> getFlow(entry: Entry<TValue, TEntries>): Flow<TValue> {
        val key = entry.getKey()

        return dataStore.data.map { it.getValue(key, entry) }
    }

    override suspend fun <TValue : Any> set(entry: Entry<TValue, TEntries>, value: TValue) {
        dataStore.updateData {
            it.toMutablePreferences().also { prefs ->
                prefs[entry.getKey()] = value
            }
        }
    }

    protected abstract fun Preferences.toSnapshot(): TSnapshot
    protected abstract fun <TValue : Any> Entry<TValue, TEntries>.getKey(): Preferences.Key<TValue>
}

fun <TValue : Any, TEntries : AppPreferences.Entries> Preferences.getValue(
    key: Preferences.Key<TValue>,
    entry: AppPreferences.Entry<TValue, TEntries>
): TValue {
    return this[key] ?: entry.defaultValue
}