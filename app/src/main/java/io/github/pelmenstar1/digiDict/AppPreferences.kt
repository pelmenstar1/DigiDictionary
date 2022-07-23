package io.github.pelmenstar1.digiDict

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.preferences: DataStore<Preferences> by preferencesDataStore("preferences")

const val DEFAULT_SCORE_POINTS_PER_CORRECT_ANSWER = 1
const val DEFAULT_SCORE_POINTS_PER_WRONG_ANSWER = 2
const val DEFAULT_USE_CUSTOM_TABS = true

val SCORE_POINTS_PER_CORRECT_ANSWER_KEY = intPreferencesKey("scorePointsPerCorrectAnswer")
val SCORE_POINTS_PER_WRONG_ANSWER_KEY = intPreferencesKey("scorePointsPerWrongAnswer")
val USE_CUSTOM_TABS_KEY = booleanPreferencesKey("useCustomTabs")

val DataStore<Preferences>.scorePointsPerCorrectAnswer: Flow<Int>
    get() = data.map { it[SCORE_POINTS_PER_CORRECT_ANSWER_KEY] ?: DEFAULT_SCORE_POINTS_PER_CORRECT_ANSWER }

val DataStore<Preferences>.scorePointsPerWrongAnswer: Flow<Int>
    get() = data.map { it[SCORE_POINTS_PER_WRONG_ANSWER_KEY] ?: DEFAULT_SCORE_POINTS_PER_WRONG_ANSWER }

val DataStore<Preferences>.useCustomTabs: Flow<Boolean>
    get() = data.map { it[USE_CUSTOM_TABS_KEY] ?: DEFAULT_USE_CUSTOM_TABS }