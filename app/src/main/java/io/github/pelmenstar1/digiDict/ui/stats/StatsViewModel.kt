package io.github.pelmenstar1.digiDict.ui.stats

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.pelmenstar1.digiDict.stats.CommonStats
import io.github.pelmenstar1.digiDict.stats.CommonStatsProvider
import io.github.pelmenstar1.digiDict.time.CurrentEpochSecondsProvider
import io.github.pelmenstar1.digiDict.utils.DataLoadStateManager
import javax.inject.Inject

@HiltViewModel
class StatsViewModel @Inject constructor(
    private val statsProvider: CommonStatsProvider,
    private val currentEpochSecondsProvider: CurrentEpochSecondsProvider
) : ViewModel() {
    private val resultStateManager = DataLoadStateManager<CommonStats>(TAG)

    val resultStateFlow = resultStateManager.buildFlow(viewModelScope) {
        fromAction {
            val currentEpochSeconds = currentEpochSecondsProvider.currentEpochSeconds()

            statsProvider.compute(currentEpochSeconds)
        }
    }

    fun retryComputeStats() {
        resultStateManager.retry()
    }

    companion object {
        private const val TAG = "StatsViewModel"
    }
}