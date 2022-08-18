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
import io.github.pelmenstar1.digiDict.R
import io.github.pelmenstar1.digiDict.common.*
import io.github.pelmenstar1.digiDict.databinding.DialogAddBadgeBinding
import javax.inject.Inject

@AndroidEntryPoint
class AddEditBadgeDialog : MaterialDialogFragment() {
    var onSubmit: ((String) -> Unit)? = null
    val currentBadgeName: String?
        get() = arguments?.getString(ARGS_CURRENT_BADGE_NAME)

    private val viewModel by viewModels<AddEditBadgeDialogViewModel>()

    @Inject
    lateinit var inputMessageMapper: MessageMapper<AddEditBadgeInputMessage>

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val vm = viewModel
        val ls = lifecycleScope
        val curBadgeName = currentBadgeName

        val binding = DialogAddBadgeBinding.inflate(inflater, container, false)
        vm.currentBadgeName = curBadgeName

        binding.addBadgeDialogNameInputLayout.also {
            ls.launchErrorFlowCollector(it, vm.inputErrorFlow, inputMessageMapper)
        }

        binding.addBadgeDialogNameInput.also {
            if (curBadgeName != null) {
                it.setText(curBadgeName)
            }

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
            ls.launchSetEnabledIfEquals(it, value = null, vm.inputErrorFlow)

            it.setText(if (curBadgeName != null) R.string.applyChanges else R.string.add)
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

    companion object {
        private const val ARGS_CURRENT_BADGE_NAME =
            "io.github.pelmenstar1.digiDict.ui.manageRecordBadges.AddEditBadgeDialog.currentBadgeName"

        /**
         * Creates required for [AddEditBadgeDialog] arguments. If [currentBadgeName] is not null, it means the dialog will
         * be **editing** badge with this name.
         */
        fun args(currentBadgeName: String? = null) = Bundle(1).apply {
            putString(ARGS_CURRENT_BADGE_NAME, currentBadgeName)
        }
    }
}