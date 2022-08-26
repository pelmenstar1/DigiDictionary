package io.github.pelmenstar1.digiDict.ui.manageRecordBadges

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.inputmethod.EditorInfo
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import dagger.hilt.android.AndroidEntryPoint
import io.github.pelmenstar1.digiDict.R
import io.github.pelmenstar1.digiDict.common.MaterialDialogFragment
import io.github.pelmenstar1.digiDict.common.MessageMapper
import io.github.pelmenstar1.digiDict.common.trimToString
import io.github.pelmenstar1.digiDict.common.ui.ColorPaletteView
import io.github.pelmenstar1.digiDict.common.ui.launchErrorFlowCollector
import io.github.pelmenstar1.digiDict.common.ui.launchSetEnabledIfEquals
import io.github.pelmenstar1.digiDict.data.RecordBadgeInfo
import io.github.pelmenstar1.digiDict.databinding.DialogAddBadgeBinding
import javax.inject.Inject

@AndroidEntryPoint
class AddEditBadgeDialog : MaterialDialogFragment() {
    var onSubmit: ((RecordBadgeInfo) -> Unit)? = null
    val currentBadge: RecordBadgeInfo?
        get() = arguments?.getParcelable(ARGS_CURRENT_BADGE)

    private val viewModel by viewModels<AddEditBadgeDialogViewModel>()

    @Inject
    lateinit var inputMessageMapper: MessageMapper<AddEditBadgeInputMessage>
    private lateinit var colorPaletteView: ColorPaletteView

    override fun createDialogView(layoutInflater: LayoutInflater, savedInstanceState: Bundle?): View {
        val vm = viewModel
        val ls = lifecycleScope

        val currentBadge = currentBadge
        val currentBadgeName = currentBadge?.name
        vm.currentBadgeName = currentBadgeName

        return DialogAddBadgeBinding.inflate(layoutInflater, null, false).let { b ->
            val colorPalette = b.addBadgeDialogColorPalette
            colorPaletteView = colorPalette

            var selectLastColorNeeded = currentBadge == null

            if (currentBadge != null) {
                val isSelected = colorPalette.selectColor(currentBadge.outlineColor)

                selectLastColorNeeded = !isSelected
            }

            if (selectLastColorNeeded) {
                colorPalette.selectLastColor()
            }

            b.addBadgeDialogNameInputLayout.also {
                ls.launchErrorFlowCollector(it, vm.nameErrorFlow, inputMessageMapper)
            }

            b.addBadgeDialogNameInput.also {
                if (currentBadgeName != null) {
                    it.setText(currentBadgeName)
                }

                it.setOnEditorActionListener { _, actionId, _ ->
                    if (actionId == EditorInfo.IME_ACTION_DONE || actionId == EditorInfo.IME_ACTION_NEXT) {
                        notifySubmit()

                        true
                    } else false
                }

                it.addTextChangedListener { text -> vm.name = text.trimToString() }
            }

            b.addBadgeDialogAddButton.also {
                ls.launchSetEnabledIfEquals(it, value = null, vm.nameErrorFlow)

                it.setText(if (currentBadgeName != null) R.string.applyChanges else R.string.add)
                it.setOnClickListener { notifySubmit() }
            }

            b.root
        }
    }

    private fun notifySubmit() {
        val name = viewModel.name
        val outlineColor = colorPaletteView.selectedColor

        onSubmit?.invoke(RecordBadgeInfo(currentBadge?.id ?: 0, name, outlineColor))

        dismiss()
    }

    companion object {
        private const val ARGS_CURRENT_BADGE =
            "io.github.pelmenstar1.digiDict.ui.manageRecordBadges.AddEditBadgeDialog.currentBadge"

        /**
         * Creates required for [AddEditBadgeDialog] arguments. If [currentBadge] is not null, it means the dialog will
         * be **editing** badge with this name.
         */
        fun args(currentBadge: RecordBadgeInfo? = null) = Bundle(1).apply {
            putParcelable(ARGS_CURRENT_BADGE, currentBadge)
        }
    }
}