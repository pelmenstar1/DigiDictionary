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
            onRemove = { name ->
                MaterialAlertDialogBuilder(context)
                    .setMessage(R.string.manageRecordBadges_removeWarning)
                    .setPositiveButton(android.R.string.ok) { _, _ -> vm.remove(name) }
                    .setNegativeButton(android.R.string.cancel, NO_OP_DIALOG_ON_CLICK_LISTENER)
                    .show()
            }
        )

        vm.onRemoveError.handler = showSnackbarEventHandler(container, R.string.manageRecordBadges_removeError)
        vm.onAddError.handler = showSnackbarEventHandler(container, R.string.manageRecordBadges_addError)

        binding.manageRecordBadgesRecyclerView.also {
            it.adapter = adapter
            it.layoutManager = LinearLayoutManager(context)
            it.addItemDecoration(DividerItemDecoration(context, DividerItemDecoration.VERTICAL))
        }

        binding.manageRecordBadgesContainer.setupLoadStateFlow(lifecycleScope, vm) {
            adapter.submitData(it)
        }

        binding.manageRecordBadgesAdd.also {
            it.setOnClickListener { showAddBadgeDialog() }
        }

        initAddBadgeDialogIfShown()

        return binding.root
    }

    private fun showAddBadgeDialog() {
        AddBadgeDialog().also {
            initAddBadgeDialog(it)

            it.show(childFragmentManager, ADD_BADGE_DIALOG_TAG)
        }
    }

    private fun initAddBadgeDialogIfShown() {
        val fm = childFragmentManager

        fm.findFragmentByTag(ADD_BADGE_DIALOG_TAG)?.also {
            initAddBadgeDialog(it as AddBadgeDialog)
        }
    }

    private fun initAddBadgeDialog(dialog: AddBadgeDialog) {
        dialog.onSubmit = { name -> viewModel.add(name) }
    }

    companion object {
        private const val ADD_BADGE_DIALOG_TAG = "AddBadgeDialog"
    }
}