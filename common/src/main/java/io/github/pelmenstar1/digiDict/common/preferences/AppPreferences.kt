package io.github.pelmenstar1.digiDict.common.preferences

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

typealias AppPreferencesGetEntry<TValue, TEntries> = TEntries.() -> AppPreferences.Entry<TValue, TEntries>

abstract class AppPreferences<TEntries : AppPreferences.Entries, TSnapshot : AppPreferences.Snapshot<TEntries>> {
    interface Entries
    data class Entry<out TValue : Any, TEntries : Entries>(val defaultValue: TValue)

    interface Snapshot<TEntries : Entries> {
        operator fun <TValue : Any> get(entry: Entry<TValue, TEntries>): TValue
    }

    abstract val entries: TEntries

    abstract fun getSnapshotFlow(): Flow<TSnapshot>
    abstract suspend fun <TValue : Any> set(entry: Entry<TValue, TEntries>, value: TValue)

    open suspend fun <TValue : Any> get(entry: Entry<TValue, TEntries>): TValue {
        val snapshot = getSnapshotFlow().first()

        return snapshot[entry]
    }

    open fun <TValue : Any> getFlow(entry: Entry<TValue, TEntries>): Flow<TValue> {
        return getSnapshotFlow().map { it[entry] }
    }
}

suspend inline fun <TValue : Any, TEntries : AppPreferences.Entries, TSnapshot : AppPreferences.Snapshot<TEntries>> AppPreferences<TEntries, TSnapshot>.get(
    getEntry: AppPreferencesGetEntry<TValue, TEntries>
): TValue {
    return get(entries.getEntry())
}

fun <TValue : Any, TEntries : AppPreferences.Entries> TEntries.entry(defaultValue: TValue): AppPreferences.Entry<TValue, TEntries> {
    return AppPreferences.Entry(defaultValue)
}