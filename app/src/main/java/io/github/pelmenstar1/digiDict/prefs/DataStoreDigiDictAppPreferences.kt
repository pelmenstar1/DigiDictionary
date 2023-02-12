package io.github.pelmenstar1.digiDict.prefs

import android.content.Context
import android.os.Build
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

val Context.dataStorePreferences: DataStore<Preferences> by preferencesDataStore("preferences")

class DataStoreDigiDictAppPreferences(private val dataStore: DataStore<Preferences>) : DigiDictAppPreferences() {
    private val prefsFlow = dataStore.data
    private val snapshotFlow = prefsFlow.map { it.toSnapshot() }

    override fun getSnapshotFlow() = snapshotFlow

    override suspend fun <TValue : Any> get(entry: Entry<TValue, Entries>): TValue {
        return prefsFlow.first().getValue(entry)
    }

    override fun <TValue : Any> getFlow(entry: Entry<TValue, Entries>): Flow<TValue> {
        return prefsFlow.map { it.getValue(entry) }
    }

    override suspend fun <TValue : Any> set(entry: Entry<TValue, Entries>, value: TValue) {
        dataStore.updateData {
            it.toMutablePreferences().also { prefs ->
                prefs[entry.getKey()] = value
            }
        }
    }

    private fun Preferences.toSnapshot(): Snapshot {
        val scorePointsPerCorrectAnswer = getValue { scorePointsPerCorrectAnswer }
        val scorePointsPerWrongAnswer = getValue { scorePointsPerWrongAnswer }
        val useCustomTabs = getValue { useCustomTabs }
        val remindItemsSize = getValue { remindItemsSize }
        val remindShowMeaning = getValue { remindShowMeaning }
        val widgetListMaxSize = getValue { widgetListMaxSize }

        var recordBreakStrategy = -1
        var recordHyphenationFrequency = -1

        if (Build.VERSION.SDK_INT >= 23) {
            recordBreakStrategy = getValue { recordTextBreakStrategy }
            recordHyphenationFrequency = getValue { recordTextHyphenationFrequency }
        }

        return Snapshot(
            scorePointsPerCorrectAnswer,
            scorePointsPerWrongAnswer,
            useCustomTabs,
            remindItemsSize,
            remindShowMeaning,
            widgetListMaxSize,
            recordBreakStrategy,
            recordHyphenationFrequency
        )
    }

    @Suppress("UNCHECKED_CAST")
    private fun <TValue : Any> Entry<TValue, Entries>.getKey(): Preferences.Key<TValue> {
        if (Build.VERSION.SDK_INT >= 23) {
            when (this) {
                Entries.recordTextBreakStrategy -> return RECORD_TEXT_BREAK_STRATEGY as Preferences.Key<TValue>
                Entries.recordTextHyphenationFrequency -> return RECORD_TEXT_HYPHENATION_FREQUENCY as Preferences.Key<TValue>
            }
        }

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

    private fun <TValue : Any> Preferences.getValue(entry: Entry<TValue, Entries>): TValue {
        return this[entry.getKey()] ?: entry.defaultValue
    }

    private inline fun <TValue : Any> Preferences.getValue(getEntry: Entries.() -> Entry<TValue, Entries>): TValue {
        return getValue(Entries.getEntry())
    }

    companion object {
        private val SCORE_POINTS_PER_CORRECT_ANSWER_KEY = intPreferencesKey("scorePointsPerCorrectAnswer")
        private val SCORE_POINTS_PER_WRONG_ANSWER_KEY = intPreferencesKey("scorePointsPerWrongAnswer")
        private val USE_CUSTOM_TABS_KEY = booleanPreferencesKey("useCustomTabs")
        private val REMIND_ITEMS_SIZE_KEY = intPreferencesKey("remindItemsSize")
        private val REMIND_SHOW_MEANING_KEY = booleanPreferencesKey("remindShowMeaning")
        private val WIDGET_LIST_MAX_SIZE_KEY = intPreferencesKey("widgetListMaxSize")
        private val RECORD_TEXT_BREAK_STRATEGY = intPreferencesKey("recordTextBreakStrategy")
        private val RECORD_TEXT_HYPHENATION_FREQUENCY = intPreferencesKey("recordTextHyphenationFrequency")
    }
}