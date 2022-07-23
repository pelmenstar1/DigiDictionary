package io.github.pelmenstar1.digiDict.ui.stats

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import dagger.hilt.android.AndroidEntryPoint
import io.github.pelmenstar1.digiDict.R
import io.github.pelmenstar1.digiDict.databinding.FragmentStatsBinding
import io.github.pelmenstar1.digiDict.utils.launchFlowCollector
import io.github.pelmenstar1.digiDict.utils.setFormattedText

@AndroidEntryPoint
class StatsFragment : Fragment() {
    private val viewModel by viewModels<StatsViewModel>()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val context = requireContext()
        val vm = viewModel
        val res = context.resources

        val binding = FragmentStatsBinding.inflate(inflater, container, false)

        val recordsCountFormat = res.getString(R.string.recordsCountFormat)
        val recordsAddedLast24HoursFormat = res.getString(R.string.recordsAdded_last24HoursFormat)
        val recordsAddedLast7DaysFormat = res.getString(R.string.recordsAdded_last7DaysFormat)
        val recordsAddedLast31DaysFormat = res.getString(R.string.recordsAdded_last31DaysFormat)

        lifecycleScope.run {
            launchFlowCollector(vm.countFlow) { count ->
                binding.statsCount.setFormattedText(recordsCountFormat, count)
            }

            launchFlowCollector(vm.additionStatsFlow) { stats ->
                if (stats != null) {
                    binding.run {
                        statsRecordsAddedLast24Hours.setFormattedText(
                            recordsAddedLast24HoursFormat,
                            stats.last24Hours
                        )

                        statsRecordsAddedLast7Days.setFormattedText(
                            recordsAddedLast7DaysFormat,
                            stats.last7Days
                        )

                        statsRecordsAddedLast31Days.setFormattedText(
                            recordsAddedLast31DaysFormat,
                            stats.last31Days
                        )
                    }
                }
            }

        }

        return binding.root
    }
}