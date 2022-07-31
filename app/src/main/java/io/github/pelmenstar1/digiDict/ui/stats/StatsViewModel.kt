package io.github.pelmenstar1.digiDict.ui.stats

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.pelmenstar1.digiDict.stats.CommonStats
import io.github.pelmenstar1.digiDict.stats.CommonStatsProvider
import io.github.pelmenstar1.digiDict.time.CurrentEpochSecondsProvider
import io.github.pelmenstar1.digiDict.utils.Event
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class StatsViewModel @Inject constructor(
    private val statsProvider: CommonStatsProvider,
    private val currentEpochSecondsProvider: CurrentEpochSecondsProvider
) : ViewModel() {
    private val _resultFlow = MutableStateFlow<CommonStats?>(null)
    val resultFlow = _resultFlow.asStateFlow()

    val onLoadError = Event()

    init {
        computeStats()
    }

    fun computeStats() {
        viewModelScope.launch(Dispatchers.Default) {
            try {
                val currentEpochSeconds = currentEpochSecondsProvider.currentEpochSeconds()
                val result = statsProvider.compute(currentEpochSeconds)

                _resultFlow.value = result
            } catch (e: Exception) {
                Log.e(TAG, "during compute()", e)

                onLoadError.raiseOnMainThread()
            }
        }
    }

    companion object {
        private const val TAG = "StatsViewModel"
    }
}