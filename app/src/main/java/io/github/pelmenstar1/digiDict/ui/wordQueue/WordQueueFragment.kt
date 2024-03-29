package io.github.pelmenstar1.digiDict.ui.wordQueue

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import dagger.hilt.android.AndroidEntryPoint
import io.github.pelmenstar1.digiDict.R
import io.github.pelmenstar1.digiDict.common.android.showSnackbarEventHandlerOnError
import io.github.pelmenstar1.digiDict.common.tryGetSuccess
import io.github.pelmenstar1.digiDict.data.WordQueueEntry
import io.github.pelmenstar1.digiDict.databinding.FragmentWordQueueBinding
import io.github.pelmenstar1.digiDict.ui.wordQueue.addWordDialog.AddWordToQueueDialogFragment

@AndroidEntryPoint
class WordQueueFragment : Fragment() {
    private val viewModel by viewModels<WordQueueViewModel>()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val vm = viewModel

        val binding = FragmentWordQueueBinding.inflate(inflater, container, false)

        val stateContainer = binding.root
        val addButton = binding.wordQueueAddButton
        val recyclerView = binding.wordQueueRecyclerView
        val emptyTextView = binding.wordQueueEmptyTextView

        showSnackbarEventHandlerOnError(
            vm.removeFromQueueAction,
            container,
            msgId = R.string.wordQueue_failedToRemoveFromQueue,
        )

        val adapter = WordQueueAdapter(
            onAddMeaning = ::navigateToAddEditRecord,
            onRemoveFromQueue = { entry -> vm.removeFromQueue(entry.id) }
        )

        recyclerView.adapter = adapter
        recyclerView.layoutManager = LinearLayoutManager(context)

        addButton.setOnClickListener { showAddToQueueDialog() }

        stateContainer.setupLoadStateFlow(lifecycleScope, vm) {
            if (it.isEmpty()) {
                emptyTextView.visibility = View.VISIBLE
                adapter.submitEmpty()
            } else {
                emptyTextView.visibility = View.GONE
                adapter.submitItems(it)
            }
        }

        return stateContainer
    }

    private fun showAddToQueueDialog() {
        val dialog = AddWordToQueueDialogFragment().apply {
            cachedEntries = viewModel.dataStateFlow.tryGetSuccess()
        }

        dialog.show(childFragmentManager, null)
    }

    private fun navigateToAddEditRecord(entry: WordQueueEntry) {
        val directions = WordQueueFragmentDirections.actionWordQueueFragmentToAddEditRecordFragment(
            initialExpression = entry.word
        )

        findNavController().navigate(directions)
    }
}