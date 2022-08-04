package io.github.pelmenstar1.digiDict.ui.stats

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import dagger.hilt.android.AndroidEntryPoint
import io.github.pelmenstar1.digiDict.databinding.FragmentStatsBinding
import io.github.pelmenstar1.digiDict.utils.DataLoadState
import io.github.pelmenstar1.digiDict.utils.launchFlowCollector

@AndroidEntryPoint
class StatsFragment : Fragment() {
    private val viewModel by viewModels<StatsViewModel>()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val vm = viewModel

        val binding = FragmentStatsBinding.inflate(inflater, container, false)

        with(binding) {
            statsErrorContainer.setOnRetryListener {
                vm.retryComputeStats()
            }

            lifecycleScope.launchFlowCollector(vm.resultStateFlow) {
                when (it) {
                    is DataLoadState.Loading -> {
                        statsLoadingIndicator.visibility = View.VISIBLE
                        statsErrorContainer.visibility = View.GONE
                        statsContentContainer.visibility = View.GONE
                    }
                    is DataLoadState.Error -> {
                        statsErrorContainer.visibility = View.VISIBLE
                        statsLoadingIndicator.visibility = View.GONE
                        statsContentContainer.visibility = View.GONE
                    }
                    is DataLoadState.Success -> {
                        val (result) = it
                        val (count, additionStats) = result

                        statsContentContainer.visibility = View.VISIBLE
                        statsLoadingIndicator.visibility = View.GONE
                        statsErrorContainer.visibility = View.GONE

                        statsCountView.setValue(count)
                        statsRecordsAddedLast24HoursView.setValue(additionStats.last24Hours)
                        statsRecordsAddedLast7DaysView.setValue(additionStats.last7Days)
                        statsRecordsAddedLast31DaysView.setValue(additionStats.last31Days)
                    }
                }
            }
        }

        return binding.root
    }
}