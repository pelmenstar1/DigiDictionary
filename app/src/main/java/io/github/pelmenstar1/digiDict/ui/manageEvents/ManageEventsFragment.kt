package io.github.pelmenstar1.digiDict.ui.manageEvents

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import dagger.hilt.android.AndroidEntryPoint
import io.github.pelmenstar1.digiDict.R
import io.github.pelmenstar1.digiDict.common.android.showSnackbarEventHandlerOnError
import io.github.pelmenstar1.digiDict.common.launchFlowCollector
import io.github.pelmenstar1.digiDict.common.ui.showAlertDialog
import io.github.pelmenstar1.digiDict.databinding.FragmentManageEventsBinding

@AndroidEntryPoint
class ManageEventsFragment : Fragment() {
    private val viewModel by viewModels<ManageEventsViewModel>()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val context = requireContext()
        val vm = viewModel
        val ls = lifecycleScope
        val navController = findNavController()

        val binding = FragmentManageEventsBinding.inflate(inflater, container, false)

        val startEventButton = binding.manageEventsStartEventButton
        val recyclerView = binding.manageEventsRecyclerView
        val stateContainer = binding.manageEventsContainer

        showSnackbarEventHandlerOnError(
            vm.deleteAction,
            container,
            msgId = R.string.manageEvents_deleteError,
            anchorView = startEventButton
        )

        showSnackbarEventHandlerOnError(
            vm.stopEventAction,
            container,
            msgId = R.string.manageEvents_stopEventError,
            anchorView = startEventButton
        )

        val adapter = ManageEventsAdapter(::executeMenuAction, ::stopEvent)

        recyclerView.also {
            it.layoutManager = LinearLayoutManager(context)
            it.adapter = adapter
            it.addItemDecoration(DividerItemDecoration(context, DividerItemDecoration.VERTICAL))
        }

        startEventButton.also {
            it.setOnClickListener {
                val directions = ManageEventsFragmentDirections.actionManageEventsFragmentToStartEditEventFragment(-1)

                navController.navigate(directions)
            }
        }

        ls.run {
            launchFlowCollector(vm.isStartEventEnabledFlow) { isEnabled ->
                startEventButton.isEnabled = isEnabled
            }
        }

        stateContainer.setupLoadStateFlow(ls, vm) { events ->
            adapter.submitElements(events)
        }

        return binding.root
    }

    private fun requestDelete(itemId: Int) {
        showAlertDialog(R.string.manageEvents_deleteWarning) {
            viewModel.delete(itemId)
        }
    }

    private fun stopEvent(itemId: Int) {
        viewModel.stopEvent(itemId)
    }

    private fun executeMenuAction(itemId: Int, actionId: Int) {
        when (actionId) {
            R.id.manageEvents_itemDelete -> {
                requestDelete(itemId)
            }
            R.id.manageEvents_itemEdit -> {
                val directions =
                    ManageEventsFragmentDirections.actionManageEventsFragmentToStartEditEventFragment(itemId)

                findNavController().navigate(directions)
            }
        }
    }
}