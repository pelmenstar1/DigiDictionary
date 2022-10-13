package io.github.pelmenstar1.digiDict.prefs

import io.github.pelmenstar1.digiDict.common.preferences.AppPreferences
import io.github.pelmenstar1.digiDict.common.preferences.entry

abstract class DigiDictAppPreferences :
    AppPreferences<DigiDictAppPreferences.Entries, DigiDictAppPreferences.Snapshot>() {
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

    final override val entries = Entries
}