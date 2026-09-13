package io.github.pelmenstar1.digiDict.ui.home

import android.os.Build
import android.os.Bundle
import android.text.TextPaint
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import androidx.activity.OnBackPressedCallback
import androidx.core.view.MenuProvider
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.navigation.fragment.findNavController
import androidx.navigation.ui.onNavDestinationSelected
import androidx.paging.LoadState
import androidx.recyclerview.widget.LinearLayoutManager
import dagger.hilt.android.AndroidEntryPoint
import io.github.pelmenstar1.digiDict.R
import io.github.pelmenstar1.digiDict.common.DataLoadState
import io.github.pelmenstar1.digiDict.common.StringFormatter
import io.github.pelmenstar1.digiDict.common.android.TextBreakAndHyphenationInfoSource
import io.github.pelmenstar1.digiDict.common.filterTrue
import io.github.pelmenstar1.digiDict.common.android.launchFlowCollector
import io.github.pelmenstar1.digiDict.common.ui.OptionsBar
import io.github.pelmenstar1.digiDict.data.RecordSortType
import io.github.pelmenstar1.digiDict.databinding.FragmentHomeBinding
import io.github.pelmenstar1.digiDict.databinding.RecordLoadingErrorAndProgressMergeBinding
import io.github.pelmenstar1.digiDict.formatters.RecordSearchPropertySetFormatter
import io.github.pelmenstar1.digiDict.search.RecordSearchPropertySet
import io.github.pelmenstar1.digiDict.ui.home.search.HomeSearchAdapter
import io.github.pelmenstar1.digiDict.ui.home.search.HomeSearchEditText
import io.github.pelmenstar1.digiDict.ui.misc.RecordSortTypeDialogFragment
import io.github.pelmenstar1.digiDict.ui.paging.AppPagingAdapter
import io.github.pelmenstar1.digiDict.ui.paging.AppPagingLoadStateAdapter
import io.github.pelmenstar1.digiDict.ui.record.RecordTextPrecomputeParams
import kotlinx.coroutines.flow.combineTransform
import kotlinx.coroutines.flow.flatMapConcat
import javax.inject.Inject

@AndroidEntryPoint
class HomeFragment : Fragment() {
    private val viewModel by viewModels<HomeViewModel>()

    private var searchMenuItem: MenuItem? = null

    @Inject
    lateinit var recordSortTypeStringFormatter: StringFormatter<RecordSortType>

    @Inject
    lateinit var recordRecordSearchPropertySetFormatter: RecordSearchPropertySetFormatter

    @Inject
    lateinit var textBreakAndHyphenationInfoSource: TextBreakAndHyphenationInfoSource

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val navController = findNavController()
        val context = requireContext()

        val binding = FragmentHomeBinding.inflate(inflater, container, false)
        val recyclerView = binding.homeRecyclerView

        val onViewRecord: (Int) -> Unit = { id ->
            val directions = HomeFragmentDirections.actionHomeToViewRecord(id)

            navController.navigate(directions)
        }

        val pagingAdapter = AppPagingAdapter(onViewRecord)
        val searchAdapter = HomeSearchAdapter(onViewRecord)

        val stateContainerBinding = RecordLoadingErrorAndProgressMergeBinding.bind(binding.root)
        val retryLambda = pagingAdapter::retry

        val loadingIndicator = stateContainerBinding.loadingErrorAndProgressLoadingIndicator
        val errorContainer = stateContainerBinding.loadingErrorAndProgressErrorContainer

        errorContainer.setOnRetryListener {
            if (viewModel.isSearchActive) {
                viewModel.retrySearch()
            } else {
                pagingAdapter.retry()
            }
        }

        val addRecordButton = binding.homeAddRecord.also {
            it.setOnClickListener {
                val directions = HomeFragmentDirections.actionHomeToAddEditRecord()

                navController.navigate(directions)
            }
        }

        // homeSearchAddRecordButton is a little bit different from addRecordButton that's placed on the center bottom of the screen.
        // This button is visible when there's no results in search and suggests a user to add a new record.
        binding.homeSearchAddRecordButton.also {
            it.setOnClickListener {
                val directions = HomeFragmentDirections.actionHomeToAddEditRecord(
                    initialExpression = viewModel.searchQuery.toString()
                )

                navController.navigate(directions)
            }
        }
        val searchAddRecordContainer = binding.homeSearchAddRecordContainer
        val homeOptionsBar = binding.homeOptionsBar

        val loadStatePagingAdapter = pagingAdapter.withLoadStateHeaderAndFooter(
            AppPagingLoadStateAdapter(retryLambda),
            AppPagingLoadStateAdapter(retryLambda)
        )

        recyclerView.also {
            it.adapter = loadStatePagingAdapter
            it.layoutManager = LinearLayoutManager(context)
        }

        initSearchMenu()
        initHomeOptionsBar(binding, pagingAdapter)
        initDialogsIfShown(pagingAdapter)
        initTextBreakAndHyphenationCustomization(pagingAdapter, searchAdapter)

        viewLifecycleOwner.run {
            launchFlowCollector(viewModel.items, pagingAdapter::submitData)

            // Start searchStateFlow collection only once isActiveFlow is true .
            launchFlowCollector(
                viewModel
                    .isSearchActiveFlow
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
                        searchAddRecordContainer.isVisible = result.currentData.size == 0 && result.isMeaningfulQuery
                        searchAdapter.submitResult(result)

                        // It's better for the UX to scroll to the top in order to
                        // show the most relevant elements. It's due to the fact the scroll position remains the same
                        // between the changes. Then, when we have the query or sort type change, the scroll position will be the same and
                        // the data is changed, so we'll get into the situation when we're showing not very relevant data according
                        // to the sort type.
                        recyclerView.scrollToPosition(0)
                    }
                }
            }

            launchFlowCollector(viewModel.isSearchActiveFlow) { isActive ->
                recyclerView.adapter = if (isActive) searchAdapter else loadStatePagingAdapter

                // While search is active, there's no sense to add new record.
                addRecordButton.isVisible = !isActive

                // Hide the 'add record container' because when we start searching, the initial query is an empty string
                // which means there's no sense to show it (as query is empty). When searching ends, there's no sense
                // to keep it on the screen as well.
                searchAddRecordContainer.visibility = View.GONE

                if (isActive) {
                    // The initial state of search should be empty.
                    searchAdapter.submitEmpty()
                }

                homeOptionsBar.setPreset(if (isActive) optionsInSearchMode else optionsInDefaultMode)
            }

            launchFlowCollector(viewModel.searchProgressFlow) { progress ->
                loadingIndicator.progress = progress
            }

            launchFlowCollector(
                pagingAdapter
                    .loadStateFlow
                    .combineTransform(viewModel.isSearchActiveFlow) { state, isActive ->
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

    private fun initSearchMenu() {
        val vm = viewModel
        val imm = requireContext().getSystemService(InputMethodManager::class.java)

        // Only takes back presses while the search is running; everything else is left to the
        // navigation graph and the system, which is what keeps the predictive back animations.
        val collapseSearchOnBack = object : OnBackPressedCallback(vm.isSearchActive) {
            override fun handleOnBackPressed() {
                vm.isSearchActive = false

                searchMenuItem?.collapseActionView()
            }
        }

        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner, collapseSearchOnBack)

        viewLifecycleOwner.launchFlowCollector(vm.isSearchActiveFlow) { isActive ->
            collapseSearchOnBack.isEnabled = isActive
        }

        requireActivity().addMenuProvider(object : MenuProvider {
            override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
                menuInflater.inflate(R.menu.home_menu, menu)

                val item = menu.findItem(R.id.homeMenu_search) ?: return
                searchMenuItem = item

                // A freshly inflated menu starts collapsed, so the flag has to agree with it. It
                // can still be true here after a configuration change, since it lives in the
                // view-model while the menu does not survive.
                if (!item.isActionViewExpanded) {
                    vm.isSearchActive = false
                }

                val actionView = item.actionView as HomeSearchEditText
                actionView.addTextChangedListener { text ->
                    vm.searchQuery = text ?: ""
                }

                item.setOnActionExpandListener(object : MenuItem.OnActionExpandListener {
                    override fun onMenuItemActionExpand(item: MenuItem): Boolean {
                        vm.isSearchActive = true

                        val actionView = item.actionView as HomeSearchEditText
                        actionView.requestFocusFromTouch()

                        // It's deprecated, but at least it works. The same can't be said about setSoftInputMode().
                        // For some unknown reason, it does not work.
                        @Suppress("DEPRECATION")
                        imm?.toggleSoftInput(InputMethodManager.SHOW_IMPLICIT, 0)

                        return true
                    }

                    override fun onMenuItemActionCollapse(item: MenuItem): Boolean {
                        vm.isSearchActive = false

                        val actionView = item.actionView as HomeSearchEditText

                        // In the next time the active view is expanded, text should be empty.
                        actionView.setText("")
                        imm?.hideSoftInputFromWindow(actionView.windowToken, 0)

                        return true
                    }
                })
            }

            // The items of the 'more' submenu are navigation destinations, so let the navigation
            // graph handle them. Search is handled by its action view, not by a click.
            override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
                return when (menuItem.itemId) {
                    R.id.homeMenu_more, R.id.homeMenu_search -> false
                    else -> menuItem.onNavDestinationSelected(findNavController())
                }
            }
        }, viewLifecycleOwner, Lifecycle.State.RESUMED)
    }

    override fun onDestroyView() {
        super.onDestroyView()

        searchMenuItem = null
    }

    private fun initTextBreakAndHyphenationCustomization(
        pagingAdapter: AppPagingAdapter,
        searchAdapter: HomeSearchAdapter
    ) {
        val vm = viewModel
        val context = requireContext()

        val expressionTextPaint: TextPaint?
        val meaningTextPaint: TextPaint?

        if (Build.VERSION.SDK_INT >= 28) {
            expressionTextPaint = pagingAdapter.getExpressionTextPaintForMeasure(context)
            meaningTextPaint = pagingAdapter.getMeaningTextPaintForMeasure(context)
        } else {
            expressionTextPaint = null
            meaningTextPaint = null
        }

        viewLifecycleOwner.launchFlowCollector(textBreakAndHyphenationInfoSource.flow) { info ->
            if (Build.VERSION.SDK_INT >= 28) {
                // expressionTextPaint and meaningTextPaint will never be null on API level >= 28
                val params = RecordTextPrecomputeParams(expressionTextPaint!!, meaningTextPaint!!, info)

                vm.recordTextPrecomputeController.params = params
            }

            pagingAdapter.setTextBreakAndHyphenationInfo(info)
            searchAdapter.setTextBreakAndHyphenationInfo(info)
        }
    }

    private fun initHomeOptionsBar(binding: FragmentHomeBinding, pagingAdapter: AppPagingAdapter) {
        val optionsBar = binding.homeOptionsBar

        viewLifecycleOwner.run {
            launchFlowCollector(viewModel.sortTypeFlow) { sortType ->
                optionsBar.setOptionValue(R.id.optionsBar_sort, recordSortTypeStringFormatter.format(sortType))
            }

            launchFlowCollector(viewModel.searchPropertiesFlow) { properties ->
                optionsBar.setOptionValue(
                    R.id.optionsBar_searchProperty,
                    recordRecordSearchPropertySetFormatter.format(properties)
                )
            }
        }

        optionsBar.setOptionOnClickListener(R.id.optionsBar_sort) {
            RecordSortTypeDialogFragment.create(selectedValue = viewModel.sortType).also { dialog ->
                initSortTypeDialog(dialog, pagingAdapter)

                dialog.show(childFragmentManager, SORT_TYPE_DIALOG_TAG)
            }
        }

        optionsBar.setOptionOnClickListener(R.id.optionsBar_searchProperty) {
            HomeSearchPropertiesDialogFragment.create(viewModel.searchProperties).also { dialog ->
                initSearchPropertiesDialog(dialog)

                dialog.show(childFragmentManager, SEARCH_PROPERTIES_DIALOG_TAG)
            }
        }
    }

    private fun initSortTypeDialog(dialog: RecordSortTypeDialogFragment, pagingAdapter: AppPagingAdapter) {
        dialog.onValueSelected = { sortType ->
            viewModel.sortType = sortType

            pagingAdapter.refresh()
        }
    }

    private fun initSearchPropertiesDialog(dialog: HomeSearchPropertiesDialogFragment) {
        dialog.onValuesSelected = {
            viewModel.searchProperties = RecordSearchPropertySet(it)
        }
    }

    private fun initSortTypeDialogIfShown(pagingAdapter: AppPagingAdapter) {
        childFragmentManager.findFragmentByTag(SORT_TYPE_DIALOG_TAG)?.also {
            initSortTypeDialog(it as RecordSortTypeDialogFragment, pagingAdapter)
        }
    }

    private fun initSearchPropertiesDialogIfShown() {
        childFragmentManager.findFragmentByTag(SEARCH_PROPERTIES_DIALOG_TAG)?.also {
            initSearchPropertiesDialog(it as HomeSearchPropertiesDialogFragment)
        }
    }

    private fun initDialogsIfShown(pagingAdapter: AppPagingAdapter) {
        initSortTypeDialogIfShown(pagingAdapter)
        initSearchPropertiesDialogIfShown()
    }

    companion object {
        private const val SORT_TYPE_DIALOG_TAG = "SortTypeDialog"
        private const val SEARCH_PROPERTIES_DIALOG_TAG = "SearchPropertiesDialog"

        private val sortOption = OptionsBar.Option(
            id = R.id.optionsBar_sort,
            prefixRes = R.string.sort
        )

        private val searchPropertyOption = OptionsBar.Option(
            id = R.id.optionsBar_searchProperty,
            prefixRes = R.string.home_searchPropertyPrefix
        )

        private val optionsInDefaultMode = OptionsBar.Preset(sortOption)
        private val optionsInSearchMode = OptionsBar.Preset(sortOption, searchPropertyOption)
    }
}