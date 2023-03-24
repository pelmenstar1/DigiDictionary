package io.github.pelmenstar1.digiDict.prefs

import android.content.Context
import android.os.Build
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import io.github.pelmenstar1.digiDict.common.android.BreakStrategy
import io.github.pelmenstar1.digiDict.common.android.HyphenationFrequency
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
                if (value is Enum<*>) {
                    prefs[entry.getKeyForEnum()] = value.ordinal
                } else {
                    prefs[entry.getKeyForNonEnum()] = value
                }
            }
        }
    }

    private fun Preferences.toSnapshot(): Snapshot {
        val scorePointsPerCorrectAnswer = getNonEnumValue { scorePointsPerCorrectAnswer }
        val scorePointsPerWrongAnswer = getNonEnumValue { scorePointsPerWrongAnswer }
        val useCustomTabs = getNonEnumValue { useCustomTabs }
        val widgetListMaxSize = getNonEnumValue { widgetListMaxSize }

        var recordBreakStrategy = BreakStrategy.UNSPECIFIED
        var recordHyphenationFrequency = HyphenationFrequency.UNSPECIFIED

        if (Build.VERSION.SDK_INT >= 23) {
            recordBreakStrategy =
                getEnumValue({ recordTextBreakStrategy }, BreakStrategy::fromOrdinal)

            recordHyphenationFrequency =
                getEnumValue({ recordTextHyphenationFrequency }, HyphenationFrequency::fromOrdinal)
        }

        return Snapshot(
            scorePointsPerCorrectAnswer,
            scorePointsPerWrongAnswer,
            useCustomTabs,
            widgetListMaxSize,
            recordBreakStrategy,
            recordHyphenationFrequency
        )
    }

    @Suppress("UNCHECKED_CAST")
    private fun <TValue : Any> Entry<TValue, Entries>.getKeyForNonEnum(): Preferences.Key<TValue> {
        return when {
            this === Entries.scorePointsPerCorrectAnswer -> SCORE_POINTS_PER_CORRECT_ANSWER_KEY
            this === Entries.scorePointsPerWrongAnswer -> SCORE_POINTS_PER_WRONG_ANSWER_KEY
            this === Entries.useCustomTabs -> USE_CUSTOM_TABS_KEY
            this === Entries.widgetListMaxSize -> WIDGET_LIST_MAX_SIZE_KEY
            else -> throwIllegalPreferenceKey()
        } as Preferences.Key<TValue>
    }

    private fun <TValue : Any> Entry<TValue, Entries>.getKeyForEnum(): Preferences.Key<Int> {
        if (Build.VERSION.SDK_INT >= 23) {
            when {
                this === Entries.recordTextBreakStrategy -> return RECORD_TEXT_BREAK_STRATEGY
                this === Entries.recordTextHyphenationFrequency -> return RECORD_TEXT_HYPHENATION_FREQUENCY
            }
        }

        // There's no enums on lower API levels
        throwIllegalPreferenceKey()
    }

    @Suppress("UNCHECKED_CAST")
    private fun <TValue : Any> Preferences.getValue(entry: Entry<TValue, Entries>): TValue {
        val defaultValue = entry.defaultValue

        if (defaultValue is Enum<*>) {
            val ordinal = this[entry.getKeyForEnum()] ?: return defaultValue

            if (Build.VERSION.SDK_INT >= 23) {
                when (entry) {
                    Entries.recordTextBreakStrategy -> return BreakStrategy.fromOrdinal(ordinal) as TValue
                    Entries.recordTextHyphenationFrequency -> return HyphenationFrequency.fromOrdinal(ordinal) as TValue
                }
            }

            // There's no enums on lower API levels
            throwIllegalPreferenceKey()
        }

        return getNonEnumValue(entry)
    }

    private fun <TValue : Any> Preferences.getNonEnumValue(entry: Entry<TValue, Entries>): TValue {
        return this[entry.getKeyForNonEnum()] ?: entry.defaultValue
    }

    private inline fun <TValue : Any> Preferences.getNonEnumValue(getEntry: Entries.() -> Entry<TValue, Entries>): TValue {
        return getNonEnumValue(Entries.getEntry())
    }

    private inline fun <TValue : Any> Preferences.getEnumValue(
        entry: Entry<TValue, Entries>,
        fromOrdinal: (Int) -> TValue
    ): TValue {
        val ordinal = this[entry.getKeyForEnum()] ?: return entry.defaultValue

        return fromOrdinal(ordinal)
    }

    private inline fun <TValue : Any> Preferences.getEnumValue(
        getEntry: Entries.() -> Entry<TValue, Entries>,
        fromOrdinal: (Int) -> TValue
    ): TValue {
        return getEnumValue(Entries.getEntry(), fromOrdinal)
    }

    companion object {
        private val SCORE_POINTS_PER_CORRECT_ANSWER_KEY = intPreferencesKey("scorePointsPerCorrectAnswer")
        private val SCORE_POINTS_PER_WRONG_ANSWER_KEY = intPreferencesKey("scorePointsPerWrongAnswer")
        private val USE_CUSTOM_TABS_KEY = booleanPreferencesKey("useCustomTabs")
        private val WIDGET_LIST_MAX_SIZE_KEY = intPreferencesKey("widgetListMaxSize")
        private val RECORD_TEXT_BREAK_STRATEGY = intPreferencesKey("recordTextBreakStrategy")
        private val RECORD_TEXT_HYPHENATION_FREQUENCY = intPreferencesKey("recordTextHyphenationFrequency")

        internal fun throwIllegalPreferenceKey(): Nothing {
            throw IllegalArgumentException("Invalid preference key")
        }
    }
}