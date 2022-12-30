package io.github.pelmenstar1.digiDict.ui.addEditRecord.badge

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import dagger.hilt.android.AndroidEntryPoint
import io.github.pelmenstar1.digiDict.common.EmptyArray
import io.github.pelmenstar1.digiDict.common.android.MaterialDialogFragment
import io.github.pelmenstar1.digiDict.common.launchFlowCollector
import io.github.pelmenstar1.digiDict.data.RecordBadgeInfo
import io.github.pelmenstar1.digiDict.databinding.DialogBadgeSelectorBinding
import io.github.pelmenstar1.digiDict.ui.addEditRecord.AddEditRecordFragmentDirections

@AndroidEntryPoint
class BadgeSelectorDialog : MaterialDialogFragment() {
    private val viewModel by viewModels<BadgeSelectorDialogViewModel>()

    var onSelected: ((RecordBadgeInfo) -> Unit)? = null

    override fun createDialogView(layoutInflater: LayoutInflater, savedInstanceState: Bundle?): View {
        val context = requireContext()
        val vm = viewModel
        val ls = lifecycleScope
        val navController = findNavController()

        val binding = DialogBadgeSelectorBinding.inflate(layoutInflater, null, false)
        vm.usedBadgeIds = arguments?.getIntArray(KEY_USED_BADGE_IDS) ?: EmptyArray.INT

        binding.badgeSelectorDialogRecyclerView.also {
            val adapter = BadgeSelectorDialogAdapter(
                onItemClickListener = { badge -> notifySelected(badge) }
            )

            it.adapter = adapter
            it.layoutManager = LinearLayoutManager(context)
            it.addItemDecoration(DividerItemDecoration(context, DividerItemDecoration.VERTICAL))

            ls.launchFlowCollector(vm.validBadgesFlow) { data ->
                if (data.isEmpty()) {
                    it.visibility = View.GONE
                } else {
                    it.visibility = View.VISIBLE
                    adapter.submitData(data)
                }
            }
        }

        binding.badgeSelectorDialogManageButton.also {
            it.setOnClickListener {
                val directions = AddEditRecordFragmentDirections.actionAddEditRecordToManageRecordBadges()

                navController.navigate(directions)
            }
        }

        return binding.root
    }

    private fun notifySelected(badge: RecordBadgeInfo) {
        onSelected?.invoke(badge)

        dismiss()
    }

    companion object {
        private const val KEY_USED_BADGE_IDS =
            "io.github.pelmenstar1.digiDict.ui.addEditRecord.badge.BadgeSelectorDialog.usedBadgeIds"

        fun args(usedBadgeIds: IntArray?) = Bundle(1).apply {
            putIntArray(KEY_USED_BADGE_IDS, usedBadgeIds)
        }
    }
}