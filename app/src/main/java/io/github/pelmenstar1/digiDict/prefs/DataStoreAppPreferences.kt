package io.github.pelmenstar1.digiDict.prefs

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

val Context.dataStorePreferences: DataStore<Preferences> by preferencesDataStore("preferences")

class DataStoreAppPreferences(private val dataStore: DataStore<Preferences>) : AppPreferences() {
    override fun getSnapshotFlow(): Flow<Snapshot> {
        return dataStore.data.map {
            Snapshot(
                scorePointsPerCorrectAnswer = it.getValue(Entries.scorePointsPerCorrectAnswer),
                scorePointsPerWrongAnswer = it.getValue(Entries.scorePointsPerWrongAnswer),
                useCustomTabs = it.getValue(Entries.useCustomTabs)
            )
        }
    }

    override suspend fun <T : Any> get(entry: Entry<T>): T {
        val prefs = dataStore.data.first()

        return prefs[entry.getKey()] ?: entry.defaultValue
    }

    override suspend fun <T : Any> set(entry: Entry<T>, value: T) {
        dataStore.edit {
            it[entry.getKey()] = value
        }
    }

    companion object {
        private val SCORE_POINTS_PER_CORRECT_ANSWER_KEY = intPreferencesKey("scorePointsPerCorrectAnswer")
        private val SCORE_POINTS_PER_WRONG_ANSWER_KEY = intPreferencesKey("scorePointsPerWrongAnswer")
        private val USE_CUSTOM_TABS_KEY = booleanPreferencesKey("useCustomTabs")

        private fun <T : Any> Preferences.getValue(entry: Entry<T>): T {
            return this[entry.getKey()] ?: entry.defaultValue
        }

        @Suppress("UNCHECKED_CAST")
        private fun <T : Any> Entry<T>.getKey(): Preferences.Key<T> {
            return when {
                this === Entries.scorePointsPerCorrectAnswer -> SCORE_POINTS_PER_CORRECT_ANSWER_KEY
                this === Entries.scorePointsPerWrongAnswer -> SCORE_POINTS_PER_WRONG_ANSWER_KEY
                this === Entries.useCustomTabs -> USE_CUSTOM_TABS_KEY
                else -> throw IllegalStateException("Invalid preference entry")
            } as Preferences.Key<T>

        }
    }
}