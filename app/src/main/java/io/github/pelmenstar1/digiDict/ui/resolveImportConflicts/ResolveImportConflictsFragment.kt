package io.github.pelmenstar1.digiDict.ui.resolveImportConflicts

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import io.github.pelmenstar1.digiDict.R
import io.github.pelmenstar1.digiDict.databinding.FragmentResolveImportConflictsBinding
import io.github.pelmenstar1.digiDict.utils.getIntArrayOrThrow

@AndroidEntryPoint
class ResolveImportConflictsFragment : Fragment() {
    private val viewModel by viewModels<ResolveImportConflictsViewModel>()

    private lateinit var adapter: ResolveImportConflictsAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val context = requireContext()
        val navController = findNavController()

        val binding = FragmentResolveImportConflictsBinding.inflate(inflater, container, false)

        binding.lifecycleOwner = viewLifecycleOwner
        binding.viewModel = viewModel
        binding.navController = navController

        adapter = ResolveImportConflictsAdapter(
            onItemStateChanged = viewModel::onItemStateChanged
        )

        binding.resolveImportConflictsRecyclerView.also {
            it.adapter = adapter
            it.layoutManager = LinearLayoutManager(context)
            it.addItemDecoration(DividerItemDecoration(context, DividerItemDecoration.VERTICAL))
        }

        adapter.also { adapter ->
            adapter.submitItems(viewModel.entries)

            if(savedInstanceState != null) {
                val itemStates = savedInstanceState.getIntArrayOrThrow(SAVED_STATE_ITEM_STATES)

                adapter.setItemStates(itemStates)
            }
        }

        viewModel.onApplyChanges = {
            container?.let {
                Snackbar.make(it, R.string.importSuccess, Snackbar.LENGTH_SHORT).show()
            }

            navController.popBackStack()
        }

        return binding.root
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putIntArray(SAVED_STATE_ITEM_STATES, viewModel.entriesStates)
    }

    companion object {
        private const val SAVED_STATE_ITEM_STATES = "io.github.pelmenstar1.digiDict.ResolveImportConflictsFragment.itemStates"
    }
}