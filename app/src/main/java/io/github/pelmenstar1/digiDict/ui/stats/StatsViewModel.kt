package io.github.pelmenstar1.digiDict.ui.stats

import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.pelmenstar1.digiDict.stats.CommonStats
import io.github.pelmenstar1.digiDict.stats.CommonStatsProvider
import io.github.pelmenstar1.digiDict.time.CurrentEpochSecondsProvider
import io.github.pelmenstar1.digiDict.ui.SingleDataLoadStateViewModel
import io.github.pelmenstar1.digiDict.utils.DataLoadStateManager
import javax.inject.Inject

@HiltViewModel
class StatsViewModel @Inject constructor(
    private val statsProvider: CommonStatsProvider,
    private val currentEpochSecondsProvider: CurrentEpochSecondsProvider
) : SingleDataLoadStateViewModel<CommonStats>(TAG) {
    override fun DataLoadStateManager.FlowBuilder<CommonStats>.buildDataFlow() = fromAction {
        val currentEpochSeconds = currentEpochSecondsProvider.currentEpochSeconds()

        statsProvider.compute(currentEpochSeconds)
    }

    companion object {
        private const val TAG = "StatsViewModel"
    }
}