package io.github.pelmenstar1.digiDict.ui.manageRecordBadges

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dagger.hilt.android.AndroidEntryPoint
import io.github.pelmenstar1.digiDict.R
import io.github.pelmenstar1.digiDict.common.NO_OP_DIALOG_ON_CLICK_LISTENER
import io.github.pelmenstar1.digiDict.common.showSnackbarEventHandler
import io.github.pelmenstar1.digiDict.databinding.FragmentManageRecordBadgesBinding

@AndroidEntryPoint
class ManageRecordBadgesFragment : Fragment() {
    private val viewModel by viewModels<ManageRecordBadgesViewModel>()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val context = requireContext()
        val vm = viewModel

        val binding = FragmentManageRecordBadgesBinding.inflate(inflater, container, false)
        val adapter = ManageRecordBadgesAdapter(
            onAction = { actionId, name ->
                when (actionId) {
                    ManageRecordBadgesAdapter.ACTION_REMOVE -> {
                        showDeleteWarningDialog(name)
                    }
                    ManageRecordBadgesAdapter.ACTION_EDIT -> {
                        showAddEditBadgeDialog(currentBadgeName = name)
                    }
                }
            }
        )

        vm.onRemoveError.handler = showSnackbarEventHandler(container, R.string.manageRecordBadges_removeError)
        vm.onAddError.handler = showSnackbarEventHandler(container, R.string.manageRecordBadges_addError)
        vm.onEditError.handler = showSnackbarEventHandler(container, R.string.manageRecordBadges_editError)

        binding.manageRecordBadgesRecyclerView.also {
            it.adapter = adapter
            it.layoutManager = LinearLayoutManager(context)
            it.addItemDecoration(DividerItemDecoration(context, DividerItemDecoration.VERTICAL))
        }

        binding.manageRecordBadgesContainer.setupLoadStateFlow(lifecycleScope, vm) {
            adapter.submitData(it)
        }

        binding.manageRecordBadgesAdd.also {
            it.setOnClickListener { showAddEditBadgeDialog(currentBadgeName = null) }
        }

        initAddBadgeDialogIfShown()

        return binding.root
    }

    private fun showDeleteWarningDialog(name: String) {
        MaterialAlertDialogBuilder(requireContext())
            .setMessage(R.string.manageRecordBadges_removeWarning)
            .setPositiveButton(android.R.string.ok) { _, _ -> viewModel.remove(name) }
            .setNegativeButton(android.R.string.cancel, NO_OP_DIALOG_ON_CLICK_LISTENER)
            .show()
    }

    private fun showAddEditBadgeDialog(currentBadgeName: String?) {
        AddEditBadgeDialog().also {
            initAddEditBadgeDialog(it)

            it.arguments = AddEditBadgeDialog.args(currentBadgeName)
            it.show(childFragmentManager, ADD_BADGE_DIALOG_TAG)
        }
    }

    private fun initAddBadgeDialogIfShown() {
        val fm = childFragmentManager

        fm.findFragmentByTag(ADD_BADGE_DIALOG_TAG)?.also {
            initAddEditBadgeDialog(it as AddEditBadgeDialog)
        }
    }

    private fun initAddEditBadgeDialog(dialog: AddEditBadgeDialog) {
        dialog.onSubmit = { name ->
            val currentBadgeName = dialog.currentBadgeName

            if (currentBadgeName != null) {
                viewModel.edit(currentBadgeName, name)
            } else {
                viewModel.add(name)
            }
        }
    }

    companion object {
        private const val ADD_BADGE_DIALOG_TAG = "AddBadgeDialog"
    }
}