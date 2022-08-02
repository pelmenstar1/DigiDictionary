package io.github.pelmenstar1.digiDict.ui.home

import android.os.Bundle
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
        val onViewRecord: (Int) -> Unit = { id ->
            val directions = HomeFragmentDirections.actionHomeToViewRecord(id)

            navController.navigate(directions)
        }

        val pagingAdapter = HomeAdapter(onViewRecord = onViewRecord)
        val searchAdapter = SearchAdapter(onViewRecord = onViewRecord)

        val stateContainerBinding = HomeLoadingErrorAndProgressMergeBinding.bind(binding.root)
        val retryLambda = pagingAdapter::retry

        stateContainerBinding.homeErrorContainer.setOnRetryListener(retryLambda)

        val recyclerView = binding.homeRecyclerView
        val addRecordButton = binding.homeAddRecord.also {
            it.setOnClickListener {
                val directions = HomeFragmentDirections.actionHomeToAddEditRecord()

                navController.navigate(directions)
            }
        }

        val loadStatePagingAdapter = pagingAdapter.withLoadStateHeaderAndFooter(
            HomeLoadStateAdapter(retryLambda),
            HomeLoadStateAdapter(retryLambda)
        )

        recyclerView.also {
            it.adapter = loadStatePagingAdapter
            it.layoutManager = LinearLayoutManager(context)

            it.addItemDecoration(DividerItemDecoration(context, DividerItemDecoration.VERTICAL))
        }

        lifecycleScope.run {
            launchFlowCollector(viewModel.items, pagingAdapter::submitData)
            launchFlowCollector(viewModel.searchItems, searchAdapter::submitData)

            launchFlowCollector(GlobalSearchQueryProvider.isActiveFlow) { isActive ->
                recyclerView.adapter = if (isActive) searchAdapter else loadStatePagingAdapter

                // While search is active, there's no sense to add new record.
                addRecordButton.isVisible = !isActive

                if (!isActive) {
                    // In the next time search is active, adapter should contain no elements as in the initial state.
                    searchAdapter.submitEmpty()
                }
            }

            launchFlowCollector(pagingAdapter.loadStateFlow) {
                with(stateContainerBinding) {
                    val refresh = it.refresh

                    homeLoadingIndicator.isVisible = refresh is LoadState.Loading
                    homeErrorContainer.isVisible = refresh is LoadState.Error

                    recyclerView.isVisible = refresh is LoadState.NotLoading && !refresh.endOfPaginationReached
                }
            }
        }

        return binding.root
    }
}