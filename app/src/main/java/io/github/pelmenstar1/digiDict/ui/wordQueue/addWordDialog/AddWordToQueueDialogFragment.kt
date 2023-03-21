package io.github.pelmenstar1.digiDict.ui.wordQueue.addWordDialog

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import dagger.hilt.android.AndroidEntryPoint
import io.github.pelmenstar1.digiDict.R
import io.github.pelmenstar1.digiDict.common.StringFormatter
import io.github.pelmenstar1.digiDict.common.android.MaterialDialogFragment
import io.github.pelmenstar1.digiDict.common.android.showSnackbarEventHandlerOnError
import io.github.pelmenstar1.digiDict.common.launchFlowCollector
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

        val ls = lifecycleScope
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
            setEnabledWhenFieldValid(vm.validity, AddWordToQueueDialogViewModel.wordValidityField, ls)

            setOnClickListener { vm.addEntry() }
        }

        wordEditText.addTextChangedListener {
            vm.word = it.toStringOrEmpty()
        }

        ls.launchFlowCollector(vm.wordFlow) {
            wordEditText.setTextIfCharsChanged(it)
        }

        ls.launchFlowCollector(vm.wordErrorFlow) {
            wordInputLayout.error = it?.let(errorFormatter::format)
        }

        ls.launchFlowCollector(vm.addAction.successFlow) {
            dismiss()
        }

        return root
    }
}