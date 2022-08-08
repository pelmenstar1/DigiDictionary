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
import io.github.pelmenstar1.digiDict.utils.FixedBitSet
import kotlinx.coroutines.flow.first

@AndroidEntryPoint
class RemindRecordsFragment : Fragment() {
    private val viewModel by viewModels<RemindRecordsViewModel>()

    private lateinit var remindRecordsAdapter: RemindRecordsAdapter

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val context = requireContext()
        val binding = FragmentRemindRecordsBinding.inflate(inflater, container, false)
        val vm = viewModel

        with(binding) {
            val adapter = RemindRecordsAdapter().also {
                remindRecordsAdapter = it
            }

            remindRecordsContentRecyclerView.also {
                it.adapter = adapter
                it.layoutManager = LinearLayoutManager(context)
                it.itemAnimator = null
                it.addItemDecoration(DividerItemDecoration(context, LinearLayoutManager.VERTICAL))
            }

            remindRecordsRepeat.setOnClickListener {
                // Every time retryLoadResult() is called, resultStateFlow should receive different result
                // even if current state is Success.
                vm.retryLoadData()
            }

            var isSavedStateApplied = false

            remindRecordsContainer.setupLoadStateFlow(lifecycleScope, vm) { items ->
                val defaultRevealState = vm.showMeaningFlow.first()

                adapter.submitItems(items, defaultRevealState)

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

        return binding.root
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putParcelable(SAVED_STATE_REVEALED_STATES, remindRecordsAdapter.revealedStates)
    }

    companion object {
        private const val SAVED_STATE_REVEALED_STATES =
            "io.github.pelmenstar1.digiDict.RemindRecordsFragment.revealedStates"
    }
}