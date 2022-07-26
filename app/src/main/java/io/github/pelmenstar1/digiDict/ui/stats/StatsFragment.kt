package io.github.pelmenstar1.digiDict.ui.stats

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import io.github.pelmenstar1.digiDict.R
import io.github.pelmenstar1.digiDict.databinding.FragmentStatsBinding
import io.github.pelmenstar1.digiDict.utils.launchFlowCollector
import io.github.pelmenstar1.digiDict.utils.setFormattedText
import io.github.pelmenstar1.digiDict.utils.showLifecycleAwareSnackbar

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

        vm.onLoadError.handler = {
            binding.statsContainer.visibility = View.GONE

            if (container != null) {
                Snackbar
                    .make(container, R.string.dbError, Snackbar.LENGTH_INDEFINITE)
                    .setAction(R.string.retry) {
                        vm.computeStats()
                    }
                    .showLifecycleAwareSnackbar(lifecycle)
            }
        }

        lifecycleScope.run {
            launchFlowCollector(vm.resultFlow) { result ->
                if (result != null) {
                    val count = result.count
                    val (last24Hours, last7Days, last31Days) = result.additionStats

                    with(binding) {
                        statsContainer.visibility = View.VISIBLE

                        statsCount.setFormattedText(recordsCountFormat, count)
                        statsRecordsAddedLast24Hours.setFormattedText(
                            recordsAddedLast24HoursFormat,
                            last24Hours
                        )

                        statsRecordsAddedLast7Days.setFormattedText(
                            recordsAddedLast7DaysFormat,
                            last7Days
                        )

                        statsRecordsAddedLast31Days.setFormattedText(
                            recordsAddedLast31DaysFormat,
                            last31Days
                        )
                    }
                }
            }
        }

        return binding.root
    }
}