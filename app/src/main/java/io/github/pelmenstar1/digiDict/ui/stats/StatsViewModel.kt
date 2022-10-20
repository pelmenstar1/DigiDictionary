package io.github.pelmenstar1.digiDict.ui.stats

import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.pelmenstar1.digiDict.common.DataLoadStateManager
import io.github.pelmenstar1.digiDict.common.time.CurrentEpochSecondsProvider
import io.github.pelmenstar1.digiDict.common.time.get
import io.github.pelmenstar1.digiDict.common.ui.SingleDataLoadStateViewModel
import io.github.pelmenstar1.digiDict.stats.CommonStats
import io.github.pelmenstar1.digiDict.stats.CommonStatsProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

@HiltViewModel
class StatsViewModel @Inject constructor(
    private val statsProvider: CommonStatsProvider,
    private val currentEpochSecondsProvider: CurrentEpochSecondsProvider
) : SingleDataLoadStateViewModel<CommonStats>(TAG) {
    override val canRefreshAfterSuccess: Boolean
        get() = false

    override fun DataLoadStateManager.FlowBuilder<CommonStats>.buildDataFlow() = fromAction {
        val currentEpochSeconds = currentEpochSecondsProvider.get { Local }

        withContext(Dispatchers.IO) {
            statsProvider.compute(currentEpochSeconds)
        }
    }

    companion object {
        private const val TAG = "StatsViewModel"
    }
}