package io.github.pelmenstar1.digiDict.ui.addEditRecord.badge

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import dagger.hilt.android.AndroidEntryPoint
import io.github.pelmenstar1.digiDict.common.EmptyArray
import io.github.pelmenstar1.digiDict.common.MaterialDialogFragment
import io.github.pelmenstar1.digiDict.common.launchFlowCollector
import io.github.pelmenstar1.digiDict.databinding.DialogBadgeSelectorBinding
import io.github.pelmenstar1.digiDict.ui.addEditRecord.AddEditRecordFragmentDirections

@AndroidEntryPoint
class BadgeSelectorDialog : MaterialDialogFragment() {
    private val viewModel by viewModels<BadgeSelectorDialogViewModel>()

    private lateinit var binding: DialogBadgeSelectorBinding

    var onSelected: ((String) -> Unit)? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val context = requireContext()
        val vm = viewModel
        val ls = lifecycleScope
        val navController = findNavController()

        binding = DialogBadgeSelectorBinding.inflate(inflater, container, false)
        vm.usedBadges = arguments?.getStringArray(KEY_USED_BADGES) ?: EmptyArray.STRING

        binding.badgeSelectorDialogRecyclerView.also {
            val adapter = BadgeSelectorDialogAdapter(
                onItemClickListener = { badge -> notifySelected(badge) }
            )

            it.adapter = adapter
            it.layoutManager = LinearLayoutManager(context)
            it.addItemDecoration(DividerItemDecoration(context, DividerItemDecoration.VERTICAL))

            ls.launchFlowCollector(vm.validBadgesFlow) { data -> adapter.submitData(data) }
        }

        binding.badgeSelectorDialogManageButton.also {
            it.setOnClickListener {
                val directions = AddEditRecordFragmentDirections.actionAddEditRecordToManageRecordBadges()

                navController.navigate(directions)
            }
        }

        return binding.root
    }

    private fun notifySelected(badge: String) {
        onSelected?.invoke(badge)

        dismiss()
    }

    companion object {
        private const val KEY_USED_BADGES =
            "io.github.pelmenstar1.digiDict.ui.addEditRecord.badge.BadgeSelectorDialog.usedBadges"

        fun args(usedBadges: Array<out String>?) = Bundle(1).apply {
            putStringArray(KEY_USED_BADGES, usedBadges)
        }
    }
}