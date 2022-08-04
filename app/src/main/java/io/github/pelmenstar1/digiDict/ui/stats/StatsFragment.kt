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
            statsContainer.setupLoadStateFlow(lifecycleScope, viewModel) {
                val (count, additionStats) = it

                statsCountView.setValue(count)
                statsRecordsAddedLast24HoursView.setValue(additionStats.last24Hours)
                statsRecordsAddedLast7DaysView.setValue(additionStats.last7Days)
                statsRecordsAddedLast31DaysView.setValue(additionStats.last31Days)
            }
        }

        return binding.root
    }
}