package io.github.pelmenstar1.digiDict.ui.manageRecordBadges

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import dagger.hilt.android.AndroidEntryPoint
import io.github.pelmenstar1.digiDict.common.*
import io.github.pelmenstar1.digiDict.databinding.DialogAddBadgeBinding
import javax.inject.Inject

@AndroidEntryPoint
class AddBadgeDialog : MaterialDialogFragment() {
    var onSubmit: ((String) -> Unit)? = null

    private val viewModel by viewModels<AddBadgeDialogViewModel>()

    @Inject
    lateinit var inputMessageMapper: MessageMapper<AddBadgeInputMessage>

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val vm = viewModel
        val ls = lifecycleScope

        val binding = DialogAddBadgeBinding.inflate(inflater, container, false)

        binding.addBadgeDialogNameInputLayout.also {
            ls.launchErrorFlowCollector(it, vm.inputErrorFlow, inputMessageMapper)
        }

        binding.addBadgeDialogNameInput.also {
            it.setOnEditorActionListener { v, actionId, _ ->
                if (actionId == EditorInfo.IME_ACTION_DONE || actionId == EditorInfo.IME_ACTION_NEXT) {
                    notifySubmit(v.text.trimToString())

                    true
                } else false
            }

            it.addTextChangedListener { text ->
                vm.input = text.trimToString()
            }
        }

        binding.addBadgeDialogAddButton.also {
            ls.launchFlowCollector(vm.inputErrorFlow) { msg ->
                it.isEnabled = msg == null
            }

            it.setOnClickListener {
                notifySubmit(binding.addBadgeDialogNameInput.text.trimToString())
            }
        }

        return binding.root
    }

    private fun notifySubmit(name: String) {
        onSubmit?.invoke(name)

        dismiss()
    }
}