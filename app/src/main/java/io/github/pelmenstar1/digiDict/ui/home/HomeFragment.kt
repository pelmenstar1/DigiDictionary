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
import io.github.pelmenstar1.digiDict.common.DataLoadState
import io.github.pelmenstar1.digiDict.common.filterTrue
import io.github.pelmenstar1.digiDict.common.launchFlowCollector
import io.github.pelmenstar1.digiDict.databinding.FragmentHomeBinding
import io.github.pelmenstar1.digiDict.databinding.HomeLoadingErrorAndProgressMergeBinding
import io.github.pelmenstar1.digiDict.ui.home.search.GlobalSearchQueryProvider
import io.github.pelmenstar1.digiDict.ui.home.search.SearchAdapter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.combineTransform
import kotlinx.coroutines.flow.flatMapConcat
import kotlinx.coroutines.plus

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
        val searchAdapter =
            SearchAdapter(differScope = lifecycleScope + Dispatchers.Default, onViewRecord = onViewRecord)

        val stateContainerBinding = HomeLoadingErrorAndProgressMergeBinding.bind(binding.root)
        val retryLambda = pagingAdapter::retry

        val loadingIndicator = stateContainerBinding.homeLoadingIndicator
        val errorContainer = stateContainerBinding.homeErrorContainer

        errorContainer.setOnRetryListener {
            if (GlobalSearchQueryProvider.isActive) {
                viewModel.retrySearch()
            } else {
                pagingAdapter.retry()
            }
        }

        val recyclerView = binding.homeRecyclerView

        val addRecordButton = binding.homeAddRecord.also {
            it.setOnClickListener {
                val directions = HomeFragmentDirections.actionHomeToAddEditRecord()

                navController.navigate(directions)
            }
        }

        // homeSearchAddRecordButton is little bit different from addRecordButton that's placed on the center bottom of the screen.
        // This button is visible when there's no results in search and suggests a user to add a new record.
        binding.homeSearchAddRecordButton.also {
            it.setOnClickListener {
                val directions = HomeFragmentDirections.actionHomeToAddEditRecord(
                    initialExpression = GlobalSearchQueryProvider.query.toString()
                )

                navController.navigate(directions)
            }
        }
        val searchAddRecordContainer = binding.homeSearchAddRecordContainer

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

            // Start searchStateFlow collection only once isActiveFlow is true .
            launchFlowCollector(
                GlobalSearchQueryProvider
                    .isActiveFlow
                    .filterTrue()
                    .flatMapConcat { viewModel.searchStateFlow }
            ) {
                when (it) {
                    is DataLoadState.Loading -> {
                        loadingIndicator.isIndeterminate = false
                        loadingIndicator.visibility = View.VISIBLE
                        errorContainer.visibility = View.GONE
                        recyclerView.visibility = View.GONE
                    }
                    is DataLoadState.Error -> {
                        errorContainer.visibility = View.VISIBLE
                        loadingIndicator.visibility = View.GONE
                        recyclerView.visibility = View.GONE
                    }
                    is DataLoadState.Success -> {
                        val result = it.value

                        recyclerView.visibility = View.VISIBLE
                        loadingIndicator.visibility = View.GONE
                        errorContainer.visibility = View.GONE

                        // If query is not meaningful (contains no letters or digits), 'add record button' is not shown
                        // because such expressions are forbidden.
                        searchAddRecordContainer.isVisible = result.data.size == 0 && result.isMeaningfulQuery
                        searchAdapter.submitData(result.data)
                    }
                }
            }

            launchFlowCollector(GlobalSearchQueryProvider.isActiveFlow) { isActive ->
                recyclerView.adapter = if (isActive) searchAdapter else loadStatePagingAdapter

                // While search is active, there's no sense to add new record.
                addRecordButton.isVisible = !isActive

                // Hide the 'add record container' because when we start searching, the initial query is an empty string
                // which means there's no sense to show it (as query is empty). When searching ends, there's no sense
                // to keep it on the screen as well.
                searchAddRecordContainer.visibility = View.GONE

                if (!isActive) {
                    // In the next time search is active, adapter should contain no elements as in the initial state.
                    searchAdapter.submitEmpty()
                }
            }

            launchFlowCollector(viewModel.searchProgressFlow) { progress ->
                loadingIndicator.progress = progress
            }

            launchFlowCollector(
                pagingAdapter
                    .loadStateFlow
                    .combineTransform(GlobalSearchQueryProvider.isActiveFlow) { state, isActive ->
                        // While search is active, UI should not respond to pagingAdapter state,
                        // as it's not on the screen.
                        if (!isActive) {
                            emit(state)
                        }
                    }
            ) {
                val refresh = it.refresh

                loadingIndicator.isIndeterminate = true
                loadingIndicator.isVisible = refresh is LoadState.Loading
                errorContainer.isVisible = refresh is LoadState.Error
                recyclerView.isVisible = refresh is LoadState.NotLoading && !refresh.endOfPaginationReached
            }
        }

        return binding.root
    }
}