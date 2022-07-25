package io.github.pelmenstar1.digiDict.ui.home

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.paging.LoadState
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import dagger.hilt.android.AndroidEntryPoint
import io.github.pelmenstar1.digiDict.databinding.FragmentHomeBinding
import io.github.pelmenstar1.digiDict.databinding.HomeLoadingErrorAndProgressMergeBinding
import io.github.pelmenstar1.digiDict.utils.launchFlowCollector

@AndroidEntryPoint
class HomeFragment : Fragment() {
    private val viewModel by viewModels<HomeViewModel>()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val navController = findNavController()
        val context = requireContext()

        val binding = FragmentHomeBinding.inflate(inflater, container, false)

        val adapter = HomeAdapter(onViewRecord = { id ->
            val directions = HomeFragmentDirections.actionHomeToViewRecord(id)

            navController.navigate(directions)
        })

        val stateContainerBinding = HomeLoadingErrorAndProgressMergeBinding.bind(binding.root)

        stateContainerBinding.homeRetry.setOnClickListener {
            Log.i(TAG, "Retry loading data")
            adapter.retry()
        }

        val recyclerView = binding.homeRecyclerView

        recyclerView.also {
            val retryLambda = adapter::retry

            it.adapter = adapter.withLoadStateHeaderAndFooter(
                HomeLoadStateAdapter(retryLambda),
                HomeLoadStateAdapter(retryLambda)
            )
            it.layoutManager = LinearLayoutManager(context)

            it.addItemDecoration(DividerItemDecoration(context, DividerItemDecoration.VERTICAL))
        }

        lifecycleScope.run {
            launchFlowCollector(viewModel.items, adapter::submitData)

            launchFlowCollector(adapter.loadStateFlow) {
                with(stateContainerBinding) {
                    val refresh = it.refresh

                    homeLoadingIndicator.isVisible = refresh is LoadState.Loading
                    homeErrorContainer.isVisible = refresh is LoadState.Error

                    recyclerView.isVisible = refresh is LoadState.NotLoading && !refresh.endOfPaginationReached
                }
            }
        }

        binding.homeAddExpression.setOnClickListener {
            val directions = HomeFragmentDirections.actionHomeToAddEditRecord()

            navController.navigate(directions)
        }

        return binding.root
    }

    companion object {
        private const val TAG = "HomeFragment"
    }
}