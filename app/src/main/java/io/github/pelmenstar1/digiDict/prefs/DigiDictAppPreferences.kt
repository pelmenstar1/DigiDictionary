package io.github.pelmenstar1.digiDict.prefs

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import io.github.pelmenstar1.digiDict.common.preferences.AppPreferences
import io.github.pelmenstar1.digiDict.common.preferences.DataStoreAppPreferences
import io.github.pelmenstar1.digiDict.common.preferences.entry
import io.github.pelmenstar1.digiDict.common.preferences.getValue

val Context.dataStorePreferences: DataStore<Preferences> by preferencesDataStore("preferences")

class DigiDictAppPreferences(
    dataStore: DataStore<Preferences>
) : DataStoreAppPreferences<DigiDictAppPreferences.Entries, DigiDictAppPreferences.Snapshot>(dataStore) {
    object Entries : AppPreferences.Entries {
        val scorePointsPerCorrectAnswer = entry(defaultValue = 1)
        val scorePointsPerWrongAnswer = entry(defaultValue = 2)
        val useCustomTabs = entry(defaultValue = true)
        val remindItemsSize = entry(defaultValue = 15)
        val remindShowMeaning = entry(defaultValue = false)
        val widgetListMaxSize = entry(defaultValue = 20)
    }

    class Snapshot(
        val scorePointsPerCorrectAnswer: Int,
        val scorePointsPerWrongAnswer: Int,
        val useCustomTabs: Boolean,
        val remindItemsSize: Int,
        val remindShowMeaning: Boolean,
        val widgetListMaxSize: Int
    ) : AppPreferences.Snapshot<Entries> {
        @Suppress("UNCHECKED_CAST")
        override operator fun <TValue : Any> get(entry: Entry<TValue, Entries>): TValue {
            return when {
                entry === Entries.scorePointsPerCorrectAnswer -> scorePointsPerCorrectAnswer
                entry === Entries.scorePointsPerWrongAnswer -> scorePointsPerWrongAnswer
                entry === Entries.useCustomTabs -> useCustomTabs
                entry === Entries.remindItemsSize -> remindItemsSize
                entry === Entries.remindShowMeaning -> remindShowMeaning
                entry === Entries.widgetListMaxSize -> widgetListMaxSize
                else -> throw IllegalStateException("Invalid preference entry")
            } as TValue
        }
    }

    override val entries = Entries

    override fun Preferences.toSnapshot() = Snapshot(
        getValue(SCORE_POINTS_PER_CORRECT_ANSWER_KEY, Entries.scorePointsPerCorrectAnswer),
        getValue(SCORE_POINTS_PER_WRONG_ANSWER_KEY, Entries.scorePointsPerWrongAnswer),
        getValue(USE_CUSTOM_TABS_KEY, Entries.useCustomTabs),
        getValue(REMIND_ITEMS_SIZE_KEY, Entries.remindItemsSize),
        getValue(REMIND_SHOW_MEANING_KEY, Entries.remindShowMeaning),
        getValue(WIDGET_LIST_MAX_SIZE_KEY, Entries.widgetListMaxSize)
    )

    @Suppress("UNCHECKED_CAST")
    override fun <TValue : Any> Entry<TValue, Entries>.getKey(): Preferences.Key<TValue> {
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

    companion object {
        private val SCORE_POINTS_PER_CORRECT_ANSWER_KEY = intPreferencesKey("scorePointsPerCorrectAnswer")
        private val SCORE_POINTS_PER_WRONG_ANSWER_KEY = intPreferencesKey("scorePointsPerWrongAnswer")
        private val USE_CUSTOM_TABS_KEY = booleanPreferencesKey("useCustomTabs")
        private val REMIND_ITEMS_SIZE_KEY = intPreferencesKey("remindItemsSize")
        private val REMIND_SHOW_MEANING_KEY = booleanPreferencesKey("remindShowMeaning")
        private val WIDGET_LIST_MAX_SIZE_KEY = intPreferencesKey("widgetListMaxSize")
    }
}