package io.github.pelmenstar1.digiDict.ui.stats

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import dagger.hilt.android.AndroidEntryPoint
import io.github.pelmenstar1.digiDict.common.time.MILLIS_IN_DAY
import io.github.pelmenstar1.digiDict.databinding.FragmentStatsBinding

@AndroidEntryPoint
class StatsFragment : Fragment() {
    private val viewModel by viewModels<StatsViewModel>()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val binding = FragmentStatsBinding.inflate(inflater, container, false)

        with(binding) {
            statsContainer.setupLoadStateFlow(lifecycleScope, viewModel) { (count, additionStats) ->
                statsCountView.setValue(count)
                statsRecordsAddedLast24HoursView.setValue(additionStats.last24Hours)
                statsRecordsAddedLast7DaysView.setValue(additionStats.last7Days)
                statsRecordsAddedLast31DaysView.setValue(additionStats.last31Days)

                val todayEpochDay = System.currentTimeMillis() / MILLIS_IN_DAY

                val last31DaysData = additionStats.perDayForLast31Days
                val chartOptions = LastDaysChartHelper.createChartOptions(requireContext())

                LastDaysChartHelper.setupChart(
                    statsLast7DaysChart,
                    last31DaysData,
                    start = 24,
                    lastDays = 7,
                    todayEpochDay,
                    chartOptions
                )

                LastDaysChartHelper.setupChart(
                    statsLast31DaysChart,
                    last31DaysData,
                    start = 0,
                    lastDays = 31,
                    todayEpochDay,
                    chartOptions
                )

                YearChartHelper.setupChart(
                    statsThisYearChart,
                    additionStats.monthStatsEntriesForAlignedYear
                )
            }
        }

        return binding.root
    }
}