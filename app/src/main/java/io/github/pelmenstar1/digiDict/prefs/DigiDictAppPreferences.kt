package io.github.pelmenstar1.digiDict.prefs

import android.annotation.SuppressLint
import android.os.Build
import android.text.Layout
import androidx.annotation.RequiresApi
import io.github.pelmenstar1.digiDict.common.preferences.AppPreferences
import io.github.pelmenstar1.digiDict.common.preferences.entry

abstract class DigiDictAppPreferences :
    AppPreferences<DigiDictAppPreferences.Entries, DigiDictAppPreferences.Snapshot>() {
    @SuppressLint("NewApi")
    object Entries : AppPreferences.Entries {
        val scorePointsPerCorrectAnswer = entry(defaultValue = 1)
        val scorePointsPerWrongAnswer = entry(defaultValue = 2)
        val useCustomTabs = entry(defaultValue = true)
        val remindItemsSize = entry(defaultValue = 15)
        val remindShowMeaning = entry(defaultValue = false)
        val widgetListMaxSize = entry(defaultValue = 20)

        /**
         * Entry for saving and loading break strategy of record.
         *
         * This can be one of:
         * - `-1` if the API level < 23
         * - [Layout.BREAK_STRATEGY_SIMPLE]
         * - [Layout.BREAK_STRATEGY_BALANCED]
         * - [Layout.BREAK_STRATEGY_HIGH_QUALITY]
         *
         * On API levels < 23, this entry must not be used.
         */
        @RequiresApi(23)
        val recordTextBreakStrategy: Entry<Int, Entries>

        /**
         * Entry for saving and loading hyphenation of record.
         *
         * This can be one of:
         * - `-1` if the API level < 23
         * - [Layout.HYPHENATION_FREQUENCY_NONE]
         * - [Layout.HYPHENATION_FREQUENCY_NORMAL]
         * - [Layout.HYPHENATION_FREQUENCY_FULL]
         *
         * On API levels < 23, this entry must not be used.
         */
        @RequiresApi(23)
        val recordTextHyphenationFrequency: Entry<Int, Entries>

        init {
            if (Build.VERSION.SDK_INT >= 23) {
                recordTextBreakStrategy = entry(defaultValue = Layout.BREAK_STRATEGY_SIMPLE)
                recordTextHyphenationFrequency = entry(defaultValue = Layout.HYPHENATION_FREQUENCY_NONE)
            } else {
                // We still need to initialize them.
                recordTextBreakStrategy = entry(defaultValue = -1)
                recordTextHyphenationFrequency = entry(defaultValue = -1)
            }
        }
    }

    class Snapshot(
        val scorePointsPerCorrectAnswer: Int,
        val scorePointsPerWrongAnswer: Int,
        val useCustomTabs: Boolean,
        val remindItemsSize: Int,
        val remindShowMeaning: Boolean,
        val widgetListMaxSize: Int,

        @RequiresApi(23)
        val recordTextBreakStrategy: Int,

        @RequiresApi(23)
        val recordTextHyphenationFrequency: Int
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
                entry === Entries.remindItemsSize -> remindItemsSize
                entry === Entries.remindShowMeaning -> remindShowMeaning
                entry === Entries.widgetListMaxSize -> widgetListMaxSize
                else -> throw IllegalStateException("Invalid preference entry")
            } as TValue
        }
    }

    final override val entries = Entries
}