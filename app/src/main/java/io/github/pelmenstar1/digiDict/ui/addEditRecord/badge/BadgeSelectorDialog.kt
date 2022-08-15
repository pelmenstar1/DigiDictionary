package io.github.pelmenstar1.digiDict.ui.addEditRecord.badge

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dagger.hilt.android.AndroidEntryPoint
import io.github.pelmenstar1.digiDict.common.*
import io.github.pelmenstar1.digiDict.databinding.DialogBadgeSelectorBinding
import javax.inject.Inject


@AndroidEntryPoint
class BadgeSelectorDialog : DialogFragment() {
    private val viewModel by viewModels<BadgeSelectorDialogViewModel>()

    private lateinit var binding: DialogBadgeSelectorBinding

    @Inject
    lateinit var inputMessageMapper: MessageMapper<BadgeSelectorInputMessage>

    var onSelected: ((String) -> Unit)? = null

    private var dialogView: View? = null

    override fun getView() = dialogView

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return MaterialAlertDialogBuilder(requireContext(), theme).apply {
            dialogView = onCreateView(layoutInflater, null, savedInstanceState)

            setView(dialogView)
        }.create()
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val context = requireContext()
        val vm = viewModel
        val ls = lifecycleScope

        binding = DialogBadgeSelectorBinding.inflate(inflater, container, false)
        vm.usedBadges = arguments?.getStringArray(KEY_USED_BADGES) ?: EmptyArray.STRING

        binding.badgeSelectorDialogInputLayout.also {
            ls.launchErrorFlowCollector(it, vm.inputErrorFlow, inputMessageMapper)
        }

        binding.badgeSelectorDialogInput.also {
            it.setOnEditorActionListener { v, actionId, _ ->
                if (actionId == EditorInfo.IME_ACTION_DONE || actionId == EditorInfo.IME_ACTION_NEXT) {
                    notifySelected(v.text.trimToString())

                    true
                } else false
            }

            it.addTextChangedListener { text ->
                vm.input = text.trimToString()
            }
        }

        binding.badgeSelectorDialogRecyclerView.also {
            val adapter = BadgeSelectorDialogAdapter(
                onItemClickListener = { badge -> notifySelected(badge) }
            )

            it.adapter = adapter
            it.layoutManager = LinearLayoutManager(context)
            it.addItemDecoration(DividerItemDecoration(context, DividerItemDecoration.VERTICAL))

            ls.launchFlowCollector(vm.badgesForSearchFlow) { data -> adapter.submitData(data) }
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