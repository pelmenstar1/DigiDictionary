package io.github.pelmenstar1.digiDict.prefs

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

val Context.dataStorePreferences: DataStore<Preferences> by preferencesDataStore("preferences")

class DataStoreAppPreferences(private val dataStore: DataStore<Preferences>) : AppPreferences() {
    private val snapshotFlow = dataStore.data.map { it.toSnapshot() }

    override fun getSnapshotFlow() = snapshotFlow

    override suspend fun <T : Any> get(entry: Entry<T>): T {
        val prefs = dataStore.data.first()

        return prefs.getValue(entry.getKey(), entry)
    }

    override fun <T : Any> getFlow(entry: Entry<T>): Flow<T> {
        val key = entry.getKey()

        return dataStore.data.map { it.getValue(key, entry) }
    }

    override suspend fun <T : Any> set(entry: Entry<T>, value: T) {
        dataStore.updateData {
            it.toMutablePreferences().also { prefs ->
                prefs[entry.getKey()] = value
            }
        }
    }

    companion object {
        private val SCORE_POINTS_PER_CORRECT_ANSWER_KEY = intPreferencesKey("scorePointsPerCorrectAnswer")
        private val SCORE_POINTS_PER_WRONG_ANSWER_KEY = intPreferencesKey("scorePointsPerWrongAnswer")
        private val USE_CUSTOM_TABS_KEY = booleanPreferencesKey("useCustomTabs")
        private val REMIND_ITEMS_SIZE_KEY = intPreferencesKey("remindItemsSize")
        private val REMIND_SHOW_MEANING_KEY = booleanPreferencesKey("remindShowMeaning")
        private val WIDGET_LIST_MAX_SIZE_KEY = intPreferencesKey("widgetListMaxSize")

        internal fun Preferences.toSnapshot(): Snapshot {
            return Snapshot(
                getValue(SCORE_POINTS_PER_CORRECT_ANSWER_KEY, Entries.scorePointsPerCorrectAnswer),
                getValue(SCORE_POINTS_PER_WRONG_ANSWER_KEY, Entries.scorePointsPerWrongAnswer),
                getValue(USE_CUSTOM_TABS_KEY, Entries.useCustomTabs),
                getValue(REMIND_ITEMS_SIZE_KEY, Entries.remindItemsSize),
                getValue(REMIND_SHOW_MEANING_KEY, Entries.remindShowMeaning),
                getValue(WIDGET_LIST_MAX_SIZE_KEY, Entries.widgetListMaxSize)
            )
        }

        internal fun <T : Any> Preferences.getValue(key: Preferences.Key<T>, entry: Entry<T>): T {
            return this[key] ?: entry.defaultValue
        }

        @Suppress("UNCHECKED_CAST")
        internal fun <T : Any> Entry<T>.getKey(): Preferences.Key<T> {
            return when {
                this === Entries.scorePointsPerCorrectAnswer -> SCORE_POINTS_PER_CORRECT_ANSWER_KEY
                this === Entries.scorePointsPerWrongAnswer -> SCORE_POINTS_PER_WRONG_ANSWER_KEY
                this === Entries.useCustomTabs -> USE_CUSTOM_TABS_KEY
                this === Entries.remindItemsSize -> REMIND_ITEMS_SIZE_KEY
                this === Entries.remindShowMeaning -> REMIND_SHOW_MEANING_KEY
                this === Entries.widgetListMaxSize -> WIDGET_LIST_MAX_SIZE_KEY
                else -> throw IllegalStateException("Invalid preference entry")
            } as Preferences.Key<T>

        }
    }
}