package io.github.pelmenstar1.digiDict.ui.remindRecords

import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import dagger.hilt.android.AndroidEntryPoint
import io.github.pelmenstar1.digiDict.R
import io.github.pelmenstar1.digiDict.common.FixedBitSet
import io.github.pelmenstar1.digiDict.common.launchFlowCollector
import io.github.pelmenstar1.digiDict.common.ui.LastElementVerticalSpaceDecoration
import io.github.pelmenstar1.digiDict.databinding.FragmentRemindRecordsBinding

@AndroidEntryPoint
class RemindRecordsFragment : Fragment() {
    private val viewModel by viewModels<RemindRecordsViewModel>()

    private lateinit var remindRecordsAdapter: RemindRecordsAdapter

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val context = requireContext()
        val vm = viewModel
        val ls = lifecycleScope

        val binding = FragmentRemindRecordsBinding.inflate(inflater, container, false)

        val adapter = RemindRecordsAdapter().also {
            remindRecordsAdapter = it
        }

        val lastItemSpaceDecorHeight =
            context.resources.getDimensionPixelOffset(R.dimen.remindRecords_lastItemBottomPadding)
        val lastItemSpaceDecor = LastElementVerticalSpaceDecoration(lastItemSpaceDecorHeight)

        binding.remindRecordsContentRecyclerView.also {
            it.adapter = adapter
            it.layoutManager = LinearLayoutManager(context)
            it.itemAnimator = null

            it.addItemDecoration(lastItemSpaceDecor)
        }

        binding.remindRecordsRepeat.setOnClickListener {
            // Every time retryLoadResult() is called, resultStateFlow should receive different result
            // even if current state is Success.
            vm.retryLoadData()
        }

        var isSavedStateApplied = false

        binding.remindRecordsContainer.setupLoadStateFlow(ls, vm) { items ->
            adapter.submitItems(items)

            // submitItems rewrite revealedStates, so if we have saved state, we need to restore it
            // after submitItems.
            //
            // Saved state shouldn't be applied to the next items if it was already.
            // The fact of receiving new items makes the saved state invalid.
            if (savedInstanceState != null && !isSavedStateApplied) {
                isSavedStateApplied = true

                savedInstanceState.getParcelable<FixedBitSet>(SAVED_STATE_REVEALED_STATES)?.also { states ->
                    adapter.revealedStates = states
                }
            }
        }

        try {
            ls.launchFlowCollector(vm.showMeaningFlow) {
                adapter.setDefaultRevealState(it)
            }
        } catch (e: Exception) {
            // If we're unable to load default reveal state, that's not critical if we don't show
            // meaning when we should.
            Log.e(TAG, "failed to load default reveal state", e)
        }

        if (Build.VERSION.SDK_INT >= 23) {
            try {
                ls.launchFlowCollector(vm.breakAndHyphenationInfoSource.flow) { info ->
                    adapter.setBreakAndHyphenationInfo(info)
                }
            } catch (e: Exception) {
                // If we're unable to load break and hyphenation info, that's not critical that the formatting
                // will be a little bit off.
                Log.e(TAG, "failed to load break and hyphenation info", e)
            }
        }

        return binding.root
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putParcelable(SAVED_STATE_REVEALED_STATES, remindRecordsAdapter.revealedStates)
    }

    companion object {
        private const val TAG = "RemindRecordsFragment"

        private const val SAVED_STATE_REVEALED_STATES =
            "io.github.pelmenstar1.digiDict.RemindRecordsFragment.revealedStates"
    }
}