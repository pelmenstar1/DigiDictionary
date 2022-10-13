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

class DataStoreDigitDictAppPreferences(private val dataStore: DataStore<Preferences>) : DigiDictAppPreferences() {
    private val prefsFlow = dataStore.data
    private val snapshotFlow = prefsFlow.map { it.toSnapshot() }

    override fun getSnapshotFlow() = snapshotFlow

    override suspend fun <TValue : Any> get(entry: Entry<TValue, Entries>): TValue {
        val prefs = prefsFlow.first()

        return prefs.getValue(entry.getKey(), entry)
    }

    override fun <TValue : Any> getFlow(entry: Entry<TValue, Entries>): Flow<TValue> {
        val key = entry.getKey()

        return prefsFlow.map { it.getValue(key, entry) }
    }

    override suspend fun <TValue : Any> set(entry: Entry<TValue, Entries>, value: TValue) {
        dataStore.updateData {
            it.toMutablePreferences().also { prefs ->
                prefs[entry.getKey()] = value
            }
        }
    }

    private fun Preferences.toSnapshot() = Snapshot(
        getValue(
            SCORE_POINTS_PER_CORRECT_ANSWER_KEY,
            Entries.scorePointsPerCorrectAnswer
        ),
        getValue(
            SCORE_POINTS_PER_WRONG_ANSWER_KEY,
            Entries.scorePointsPerWrongAnswer
        ),
        getValue(
            USE_CUSTOM_TABS_KEY,
            Entries.useCustomTabs
        ),
        getValue(
            REMIND_ITEMS_SIZE_KEY,
            Entries.remindItemsSize
        ),
        getValue(
            REMIND_SHOW_MEANING_KEY,
            Entries.remindShowMeaning
        ),
        getValue(
            WIDGET_LIST_MAX_SIZE_KEY,
            Entries.widgetListMaxSize
        )
    )

    @Suppress("UNCHECKED_CAST")
    private fun <TValue : Any> Entry<TValue, Entries>.getKey(): Preferences.Key<TValue> {
        return when {
            this === Entries.scorePointsPerCorrectAnswer -> SCORE_POINTS_PER_CORRECT_ANSWER_KEY
            this === Entries.scorePointsPerWrongAnswer -> SCORE_POINTS_PER_WRONG_ANSWER_KEY
            this === Entries.useCustomTabs -> USE_CUSTOM_TABS_KEY
            this === Entries.remindItemsSize -> REMIND_ITEMS_SIZE_KEY
            this === Entries.remindShowMeaning -> REMIND_SHOW_MEANING_KEY
            this === Entries.widgetListMaxSize -> WIDGET_LIST_MAX_SIZE_KEY
            else -> throw IllegalStateException("Invalid preference entry")
        } as Preferences.Key<TValue>
    }

    private fun <TValue : Any> Preferences.getValue(
        key: Preferences.Key<TValue>,
        entry: Entry<TValue, Entries>
    ): TValue {
        return this[key] ?: entry.defaultValue
    }

    companion object {
        private val SCORE_POINTS_PER_CORRECT_ANSWER_KEY = intPreferencesKey("scorePointsPerCorrectAnswer")
        private val SCORE_POINTS_PER_WRONG_ANSWER_KEY = intPreferencesKey("scorePointsPerWrongAnswer")
        private val USE_CUSTOM_TABS_KEY = booleanPreferencesKey("useCustomTabs")
        private val REMIND_ITEMS_SIZE_KEY = intPreferencesKey("remindItemsSize")
        private val REMIND_SHOW_MEANING_KEY = booleanPreferencesKey("remindShowMeaning")
        private val WIDGET_LIST_MAX_SIZE_KEY = intPreferencesKey("widgetListMaxSize")
    }
}