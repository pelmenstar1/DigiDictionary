package io.github.pelmenstar1.digiDict.ui.remindRecords

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import dagger.hilt.android.AndroidEntryPoint
import io.github.pelmenstar1.digiDict.databinding.FragmentRemindRecordsBinding
import io.github.pelmenstar1.digiDict.utils.DataLoadState
import io.github.pelmenstar1.digiDict.utils.FixedBitSet
import io.github.pelmenstar1.digiDict.utils.launchFlowCollector

@AndroidEntryPoint
class RemindRecordsFragment : Fragment() {
    private val viewModel by viewModels<RemindRecordsViewModel>()

    private lateinit var remindRecodsAdapter: RemindRecordsAdapter

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val context = requireContext()
        val binding = FragmentRemindRecordsBinding.inflate(inflater, container, false)
        val vm = viewModel

        with(binding) {
            val adapter = RemindRecordsAdapter().also {
                remindRecodsAdapter = it
            }

            remindRecordsContentRecyclerView.also {
                it.adapter = adapter
                it.layoutManager = LinearLayoutManager(context)
                it.addItemDecoration(DividerItemDecoration(context, LinearLayoutManager.VERTICAL))
            }

            remindRecordsErrorContainer.setOnRetryListener {
                vm.retryLoadResult()
            }

            remindRecordsRepeat.setOnClickListener {
                // Every time retryLoadResult() is called, resultStateFlow should receive different result
                // even if current state is Success.
                vm.retryLoadResult()
            }

            var isSavedStateApplied = false

            lifecycleScope.launchFlowCollector(vm.resultStateFlow) {
                when (it) {
                    is DataLoadState.Loading -> {
                        remindRecordsLoadingIndicator.visibility = View.VISIBLE
                        remindRecordsErrorContainer.visibility = View.GONE
                        remindRecordsContentContainer.visibility = View.GONE
                    }
                    is DataLoadState.Error -> {
                        remindRecordsErrorContainer.visibility = View.VISIBLE
                        remindRecordsLoadingIndicator.visibility = View.GONE
                        remindRecordsContentContainer.visibility = View.GONE
                    }
                    is DataLoadState.Success -> {
                        val (items) = it

                        remindRecordsContentContainer.visibility = View.VISIBLE
                        remindRecordsErrorContainer.visibility = View.GONE
                        remindRecordsLoadingIndicator.visibility = View.GONE

                        adapter.submitItems(items)
                        adapter.concealAll()

                        // Saved state shouldn't be applied to the next items if it was already.
                        // The fact of receiving new items makes the saved state invalid.
                        if (savedInstanceState != null && !isSavedStateApplied) {
                            isSavedStateApplied = true

                            savedInstanceState.getParcelable<FixedBitSet>(SAVED_STATE_REVEALED_STATES)?.also { states ->
                                adapter.revealedStates = states
                            }
                        }
                    }
                }
            }
        }

        return binding.root
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putParcelable(SAVED_STATE_REVEALED_STATES, remindRecodsAdapter.revealedStates)
    }

    companion object {
        private const val SAVED_STATE_REVEALED_STATES =
            "io.github.pelmenstar1.digiDict.RemindRecordsFragment.revealedStates"
    }
}