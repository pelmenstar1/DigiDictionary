package io.github.pelmenstar1.digiDict.ui.settings

import io.github.pelmenstar1.digiDict.R
import io.github.pelmenstar1.digiDict.common.android.BreakStrategy
import io.github.pelmenstar1.digiDict.common.ui.selectionDialogs.SingleSelectionDialogFragment

class BreakStrategyDialogFragment : SingleSelectionDialogFragment<BreakStrategy>() {
    override val choicesRes: Int
        get() = R.array.settings_breakStrategy_dialogChoices

    override val choicesInfoRes: Int
        get() = R.array.settings_breakStrategy_dialogChoicesInfo

    override val titleRes: Int
        get() = R.string.settings_breakStrategy

    // UNSPECIFIED is not used, so -1
    override fun getValueByIndex(index: Int): BreakStrategy = BreakStrategy.fromOrdinal(index + 1)

    companion object {
        fun create(selectedValue: BreakStrategy): BreakStrategyDialogFragment {
            return BreakStrategyDialogFragment().apply {
                // UNSPECIFIED is not used, so -1
                arguments = createArguments(selectedValue.ordinal - 1)
            }
        }
    }
}