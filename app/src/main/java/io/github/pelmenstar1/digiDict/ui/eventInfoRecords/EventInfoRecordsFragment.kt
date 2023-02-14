package io.github.pelmenstar1.digiDict.ui.eventInfoRecords

import android.os.Build
import android.os.Bundle
import android.text.TextPaint
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
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import io.github.pelmenstar1.digiDict.R
import io.github.pelmenstar1.digiDict.common.MessageMapper
import io.github.pelmenstar1.digiDict.common.android.TextBreakAndHyphenationInfoSource
import io.github.pelmenstar1.digiDict.common.android.showLifecycleAwareSnackbar
import io.github.pelmenstar1.digiDict.common.launchFlowCollector
import io.github.pelmenstar1.digiDict.common.ui.OptionsBar
import io.github.pelmenstar1.digiDict.data.RecordSortType
import io.github.pelmenstar1.digiDict.databinding.FragmentEventInfoRecordsBinding
import io.github.pelmenstar1.digiDict.databinding.RecordLoadingErrorAndProgressMergeBinding
import io.github.pelmenstar1.digiDict.ui.misc.RecordSortTypeDialogFragment
import io.github.pelmenstar1.digiDict.ui.paging.AppPagingAdapter
import io.github.pelmenstar1.digiDict.ui.paging.AppPagingLoadStateAdapter
import io.github.pelmenstar1.digiDict.ui.record.RecordTextPrecomputeController
import io.github.pelmenstar1.digiDict.ui.record.RecordTextPrecomputeParams
import javax.inject.Inject

@AndroidEntryPoint
class EventInfoRecordsFragment : Fragment() {
    private val viewModel by viewModels<EventInfoRecordsViewModel>()

    @Inject
    lateinit var sortTypeMessageMapper: MessageMapper<RecordSortType>

    @Inject
    lateinit var textBreakAndHyphenationInfoSource: TextBreakAndHyphenationInfoSource

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val vm = viewModel
        val args = EventInfoRecordsFragmentArgs.fromBundle(requireArguments())
        val navController = findNavController()
        val context = requireContext()

        vm.eventId = args.id

        val binding = FragmentEventInfoRecordsBinding.inflate(inflater, container, false)
        val stateContainerBinding = RecordLoadingErrorAndProgressMergeBinding.bind(binding.root)

        val recyclerView = binding.eventInfoRecordsRecyclerView
        val optionsBar = binding.eventInfoRecordsOptionsBar
        val errorContainer = stateContainerBinding.loadingErrorAndProgressErrorContainer
        val loadingIndicator = stateContainerBinding.loadingErrorAndProgressLoadingIndicator

        val adapter = AppPagingAdapter(onViewRecord = { id ->
            val directions = EventInfoRecordsFragmentDirections.actionEventInfoRecordsFragmentViewRecordFragment(id)

            navController.navigate(directions)
        })
        val adapterRetryLambda = adapter::retry

        val loadStateAdapter = adapter.withLoadStateHeaderAndFooter(
            AppPagingLoadStateAdapter(adapterRetryLambda),
            AppPagingLoadStateAdapter(adapterRetryLambda)
        )

        initSortTypeDialogIfShown(adapter)

        optionsBar.also {
            it.setPreset(optionsBarPreset)
            it.setOptionOnClickListener(R.id.optionsBar_sort) {
                showSortTypeDialog(adapter)
            }
        }

        recyclerView.also {
            it.adapter = loadStateAdapter
            it.layoutManager = LinearLayoutManager(context)
            it.addItemDecoration(DividerItemDecoration(context, DividerItemDecoration.VERTICAL))
        }

        vm.recordTextPrecomputeController = RecordTextPrecomputeController.create(context)
        initTextBreakAndHyphenationCustomization(adapter)

        lifecycleScope.run {
            launchFlowCollector(vm.items, adapter::submitData)

            launchFlowCollector(vm.sortTypeFlow) {
                optionsBar.setOptionValue(R.id.optionsBar_sort, sortTypeMessageMapper.map(it))
            }

            launchFlowCollector(vm.isEventSuccessfullyLoadedFlow) { success ->
                recyclerView.isVisible = success

                if (!success && container != null) {
                    Snackbar.make(container, R.string.eventLoadError, Snackbar.LENGTH_INDEFINITE)
                        .setAction(io.github.pelmenstar1.digiDict.common.ui.R.string.retry) {
                            vm.retryLoadEvent()
                        }
                        .showLifecycleAwareSnackbar(lifecycle)
                }
            }

            launchFlowCollector(adapter.loadStateFlow) {
                val refresh = it.refresh

                loadingIndicator.isVisible = refresh is LoadState.Loading
                errorContainer.isVisible = refresh is LoadState.Error
                recyclerView.isVisible = refresh is LoadState.NotLoading && !refresh.endOfPaginationReached
            }
        }

        return binding.root
    }

    private fun initTextBreakAndHyphenationCustomization(pagingAdapter: AppPagingAdapter) {
        if (Build.VERSION.SDK_INT >= 23) {
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

            lifecycleScope.launchFlowCollector(textBreakAndHyphenationInfoSource.flow) { info ->
                if (Build.VERSION.SDK_INT >= 28) {
                    // expressionTextPaint and meaningTextPaint will never be null on API level >= 28
                    val params = RecordTextPrecomputeParams(expressionTextPaint!!, meaningTextPaint!!, info)

                    vm.recordTextPrecomputeController?.params = params
                }

                pagingAdapter.setTextBreakAndHyphenationInfo(info)
            }
        }
    }

    private fun showSortTypeDialog(pagingAdapter: AppPagingAdapter) {
        RecordSortTypeDialogFragment.create(selectedValue = viewModel.sortType).also {
            initSortTypeDialog(it, pagingAdapter)

            it.show(childFragmentManager, SORT_TYPE_DIALOG_TAG)
        }
    }

    private fun initSortTypeDialogIfShown(pagingAdapter: AppPagingAdapter) {
        childFragmentManager.findFragmentByTag(SORT_TYPE_DIALOG_TAG)?.also { dialog ->
            initSortTypeDialog(dialog as RecordSortTypeDialogFragment, pagingAdapter)
        }
    }

    private fun initSortTypeDialog(dialog: RecordSortTypeDialogFragment, pagingAdapter: AppPagingAdapter) {
        dialog.onValueSelected = { sortType ->
            viewModel.sortType = sortType

            pagingAdapter.refresh()
        }
    }

    companion object {
        private const val SORT_TYPE_DIALOG_TAG = "SortTypeDialog"

        private val optionsBarPreset = OptionsBar.Preset(
            OptionsBar.Option(
                id = R.id.optionsBar_sort,
                prefixRes = R.string.sort
            )
        )
    }
}