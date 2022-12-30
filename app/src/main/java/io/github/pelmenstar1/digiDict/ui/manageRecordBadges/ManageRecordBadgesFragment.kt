package io.github.pelmenstar1.digiDict.ui.manageRecordBadges

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
import io.github.pelmenstar1.digiDict.common.ui.showAlertDialog
import io.github.pelmenstar1.digiDict.data.RecordBadgeInfo
import io.github.pelmenstar1.digiDict.databinding.FragmentManageRecordBadgesBinding

@AndroidEntryPoint
class ManageRecordBadgesFragment : Fragment() {
    private val viewModel by viewModels<ManageRecordBadgesViewModel>()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val context = requireContext()
        val vm = viewModel

        val binding = FragmentManageRecordBadgesBinding.inflate(inflater, container, false)
        val adapter = ManageRecordBadgesAdapter(
            onAction = { actionId, badge ->
                when (actionId) {
                    ManageRecordBadgesAdapter.ACTION_REMOVE -> {
                        showDeleteWarningDialog(badge)
                    }
                    ManageRecordBadgesAdapter.ACTION_EDIT -> {
                        navigateToAddEditBadgeFragment(currentBadgeId = badge.id)
                    }
                }
            }
        )

        showSnackbarEventHandlerOnError(vm.removeAction, container, R.string.manageRecordBadges_removeError)

        binding.manageRecordBadgesRecyclerView.also {
            it.adapter = adapter
            it.layoutManager = LinearLayoutManager(context)
            it.addItemDecoration(DividerItemDecoration(context, DividerItemDecoration.VERTICAL))
        }

        binding.manageRecordBadgesContainer.setupLoadStateFlow(lifecycleScope, vm) {
            adapter.submitData(it)
        }

        binding.manageRecordBadgesAdd.also {
            it.setOnClickListener {
                navigateToAddEditBadgeFragment(currentBadgeId = -1)
            }
        }

        return binding.root
    }

    private fun navigateToAddEditBadgeFragment(currentBadgeId: Int) {
        val directions =
            ManageRecordBadgesFragmentDirections.actionManageRecordBadgesFragmentToAddEditBadgeFragment(currentBadgeId)

        findNavController().navigate(directions)
    }

    private fun showDeleteWarningDialog(badge: RecordBadgeInfo) {
        showAlertDialog(messageId = R.string.manageRecordBadges_removeWarning) {
            viewModel.remove(badge)
        }
    }
}