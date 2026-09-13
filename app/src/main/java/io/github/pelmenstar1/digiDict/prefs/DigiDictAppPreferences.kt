package io.github.pelmenstar1.digiDict.prefs

import io.github.pelmenstar1.digiDict.common.android.BreakStrategy
import io.github.pelmenstar1.digiDict.common.android.HyphenationFrequency
import io.github.pelmenstar1.digiDict.common.preferences.AppPreferences
import io.github.pelmenstar1.digiDict.common.preferences.entry

abstract class DigiDictAppPreferences :
    AppPreferences<DigiDictAppPreferences.Entries, DigiDictAppPreferences.Snapshot>() {
    object Entries : AppPreferences.Entries {
        val scorePointsPerCorrectAnswer = entry(name = "scorePointsPerCorrectAnswer", defaultValue = 1)
        val scorePointsPerWrongAnswer = entry(name = "scorePointsPerWrongAnswer", defaultValue = 2)
        val useCustomTabs = entry(name = "useCustomTabs", defaultValue = true)
        val widgetListMaxSize = entry(name = "widgetListMaxSize", defaultValue = 20)

        val recordTextBreakStrategy =
            entry(name = "recordTextBreakStrategy", defaultValue = BreakStrategy.SIMPLE)

        val recordTextHyphenationFrequency =
            entry(name = "recordTextHyphenationFrequency", defaultValue = HyphenationFrequency.NORMAL)
    }

    class Snapshot(
        val scorePointsPerCorrectAnswer: Int,
        val scorePointsPerWrongAnswer: Int,
        val useCustomTabs: Boolean,
        val widgetListMaxSize: Int,
        val recordTextBreakStrategy: BreakStrategy,
        val recordTextHyphenationFrequency: HyphenationFrequency
    ) : AppPreferences.Snapshot<Entries> {
        @Suppress("UNCHECKED_CAST")
        override operator fun <TValue : Any> get(entry: Entry<TValue, Entries>): TValue {
            return when {
                entry === Entries.scorePointsPerCorrectAnswer -> scorePointsPerCorrectAnswer
                entry === Entries.scorePointsPerWrongAnswer -> scorePointsPerWrongAnswer
                entry === Entries.useCustomTabs -> useCustomTabs
                entry === Entries.widgetListMaxSize -> widgetListMaxSize
                entry === Entries.recordTextBreakStrategy -> recordTextBreakStrategy
                entry === Entries.recordTextHyphenationFrequency -> recordTextHyphenationFrequency
                else -> throw IllegalStateException("Invalid preference entry")
            } as TValue
        }
    }

    final override val entries = Entries
}
