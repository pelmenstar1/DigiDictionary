package io.github.pelmenstar1.digiDict.ui.wordQueue.addWordDialog

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.viewModels
import dagger.hilt.android.AndroidEntryPoint
import io.github.pelmenstar1.digiDict.R
import io.github.pelmenstar1.digiDict.common.StringFormatter
import io.github.pelmenstar1.digiDict.common.android.MaterialDialogFragment
import io.github.pelmenstar1.digiDict.common.android.showSnackbarEventHandlerOnError
import io.github.pelmenstar1.digiDict.common.android.launchFlowCollector
import io.github.pelmenstar1.digiDict.common.toStringOrEmpty
import io.github.pelmenstar1.digiDict.common.ui.setEnabledWhenFieldValid
import io.github.pelmenstar1.digiDict.common.ui.setTextIfCharsChanged
import io.github.pelmenstar1.digiDict.data.WordQueueEntry
import io.github.pelmenstar1.digiDict.databinding.DialogAddWordToQueueBinding
import javax.inject.Inject

@AndroidEntryPoint
class AddWordToQueueDialogFragment : MaterialDialogFragment() {
    private val viewModel by viewModels<AddWordToQueueDialogViewModel>()

    @Inject
    lateinit var errorFormatter: StringFormatter<AddWordToQueueDialogError>

    var cachedEntries: Array<WordQueueEntry>? = null

    override fun createDialogView(layoutInflater: LayoutInflater, savedInstanceState: Bundle?): View {
        val binding = DialogAddWordToQueueBinding.inflate(layoutInflater, null, false)

        // A MaterialDialogFragment builds its content in onCreateDialog, so it has no view lifecycle
        // owner; the dialog lives exactly as long as the fragment.
        val owner = this
        val vm = viewModel

        val root = binding.root
        val addButton = binding.addWordToQueueDialogAddButton
        val wordInputLayout = binding.addWordToQueueDialogWordInput
        val wordEditText = binding.addWordToQueueDialogWordEditText

        showSnackbarEventHandlerOnError(
            vm.addAction,
            container = root,
            msgId = R.string.addWordToQueue_failedToAdd
        )

        vm.cachedWordEntries = cachedEntries

        addButton.apply {
            setEnabledWhenFieldValid(vm.validity, AddWordToQueueDialogViewModel.wordValidityField, owner)

            setOnClickListener { vm.addEntry() }
        }

        wordEditText.addTextChangedListener {
            vm.word = it.toStringOrEmpty()
        }

        owner.launchFlowCollector(vm.wordFlow) {
            wordEditText.setTextIfCharsChanged(it)
        }

        owner.launchFlowCollector(vm.wordErrorFlow) {
            wordInputLayout.error = it?.let(errorFormatter::format)
        }

        owner.launchFlowCollector(vm.addAction.successFlow) {
            dismiss()
        }

        return root
    }
}