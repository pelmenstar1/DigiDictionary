package io.github.pelmenstar1.digiDict.common.preferences

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

typealias AppPreferencesGetEntry<TValue, TEntries> = TEntries.() -> AppPreferences.Entry<TValue, TEntries>

abstract class AppPreferences<TEntries : AppPreferences.Entries, TSnapshot : AppPreferences.Snapshot<TEntries>> {
    interface Entries
    data class Entry<TValue : Any, TEntries : Entries>(
        val name: String,
        val valueClass: Class<TValue>,
        val defaultValue: TValue
    )

    interface Snapshot<TEntries : Entries> {
        operator fun <TValue : Any> get(entry: Entry<TValue, TEntries>): TValue
    }

    abstract val entries: TEntries

    abstract fun getSnapshotFlow(): Flow<TSnapshot>
    abstract suspend fun <TValue : Any> set(entry: Entry<TValue, TEntries>, value: TValue)

    fun getSnapshotFlow(observedEntries: Array<out Entry<*, TEntries>>): Flow<TSnapshot> {
        return getSnapshotFlow().distinctUntilChanged { old, new ->
            isEntriesChanged(old, new, observedEntries)
        }
    }

    inline fun getSnapshotFlow(observedEntries: TEntries.() -> Array<out Entry<*, TEntries>>): Flow<TSnapshot> {
        return getSnapshotFlow(observedEntries(entries))
    }

    open suspend fun <TValue : Any> get(entry: Entry<TValue, TEntries>): TValue {
        val snapshot = getSnapshotFlow().first()

        return snapshot[entry]
    }

    open fun <TValue : Any> getFlow(entry: Entry<TValue, TEntries>): Flow<TValue> {
        return getSnapshotFlow().map { it[entry] }
    }

    private fun isEntriesChanged(
        thisSnapshot: TSnapshot,
        otherSnapshot: TSnapshot,
        entries: Array<out Entry<*, TEntries>>
    ): Boolean {
        for (i in entries.indices) {
            val entry = entries[i]

            if (thisSnapshot[entry] != otherSnapshot[entry]) {
                return true
            }
        }

        return false
    }
}

suspend inline fun <TValue : Any, TEntries : AppPreferences.Entries, TSnapshot : AppPreferences.Snapshot<TEntries>> AppPreferences<TEntries, TSnapshot>.get(
    getEntry: AppPreferencesGetEntry<TValue, TEntries>
): TValue {
    return get(entries.getEntry())
}

inline fun <reified TValue : Any, TEntries : AppPreferences.Entries> TEntries.entry(
    name: String,
    defaultValue: TValue
): AppPreferences.Entry<TValue, TEntries> {
    return AppPreferences.Entry(name, TValue::class.java, defaultValue)
}