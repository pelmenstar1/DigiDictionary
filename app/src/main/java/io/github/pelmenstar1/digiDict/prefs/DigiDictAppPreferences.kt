package io.github.pelmenstar1.digiDict.prefs

import android.annotation.SuppressLint
import android.os.Build
import androidx.annotation.RequiresApi
import io.github.pelmenstar1.digiDict.common.android.BreakStrategy
import io.github.pelmenstar1.digiDict.common.android.HyphenationFrequency
import io.github.pelmenstar1.digiDict.common.preferences.AppPreferences
import io.github.pelmenstar1.digiDict.common.preferences.entry

abstract class DigiDictAppPreferences :
    AppPreferences<DigiDictAppPreferences.Entries, DigiDictAppPreferences.Snapshot>() {
    @SuppressLint("NewApi")
    object Entries : AppPreferences.Entries {
        val scorePointsPerCorrectAnswer = entry(defaultValue = 1)
        val scorePointsPerWrongAnswer = entry(defaultValue = 2)
        val useCustomTabs = entry(defaultValue = true)
        val widgetListMaxSize = entry(defaultValue = 20)

        @get:RequiresApi(23)
        val recordTextBreakStrategy: Entry<BreakStrategy, Entries>

        @get:RequiresApi(23)
        val recordTextHyphenationFrequency: Entry<HyphenationFrequency, Entries>

        init {
            if (Build.VERSION.SDK_INT >= 23) {
                recordTextBreakStrategy = entry(defaultValue = BreakStrategy.SIMPLE)
                recordTextHyphenationFrequency = entry(defaultValue = HyphenationFrequency.NORMAL)
            } else {
                // We still need to initialize them.
                recordTextBreakStrategy = entry(defaultValue = BreakStrategy.UNSPECIFIED)
                recordTextHyphenationFrequency = entry(defaultValue = HyphenationFrequency.UNSPECIFIED)
            }
        }
    }

    class Snapshot(
        val scorePointsPerCorrectAnswer: Int,
        val scorePointsPerWrongAnswer: Int,
        val useCustomTabs: Boolean,
        val widgetListMaxSize: Int,

        @RequiresApi(23)
        val recordTextBreakStrategy: BreakStrategy,

        @RequiresApi(23)
        val recordTextHyphenationFrequency: HyphenationFrequency
    ) : AppPreferences.Snapshot<Entries> {
        @Suppress("UNCHECKED_CAST")
        override operator fun <TValue : Any> get(entry: Entry<TValue, Entries>): TValue {
            if (Build.VERSION.SDK_INT >= 23) {
                when (entry) {
                    Entries.recordTextBreakStrategy -> return recordTextBreakStrategy as TValue
                    Entries.recordTextHyphenationFrequency -> return recordTextHyphenationFrequency as TValue
                }
            }

            return when {
                entry === Entries.scorePointsPerCorrectAnswer -> scorePointsPerCorrectAnswer
                entry === Entries.scorePointsPerWrongAnswer -> scorePointsPerWrongAnswer
                entry === Entries.useCustomTabs -> useCustomTabs
                entry === Entries.widgetListMaxSize -> widgetListMaxSize
                else -> throw IllegalStateException("Invalid preference entry")
            } as TValue
        }
    }

    final override val entries = Entries
}