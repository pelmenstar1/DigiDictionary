package io.github.pelmenstar1.digiDict.ui.settings

import io.github.pelmenstar1.digiDict.R
import io.github.pelmenstar1.digiDict.common.android.HyphenationFrequency
import io.github.pelmenstar1.digiDict.common.ui.selectionDialogs.SingleSelectionDialogFragment

class HyphenationDialogFragment : SingleSelectionDialogFragment<HyphenationFrequency>() {
    override val choicesRes: Int
        get() = R.array.settings_hyphenation_dialogChoices

    override val choicesInfoRes: Int
        get() = R.array.settings_hyphenation_dialogChoicesInfo

    override val titleRes: Int
        get() = R.string.settings_hyphenation

    // UNSPECIFIED is not used, so -1
    override fun getValueByIndex(index: Int): HyphenationFrequency = HyphenationFrequency.fromOrdinal(index + 1)

    companion object {
        fun create(selectedValue: HyphenationFrequency): HyphenationDialogFragment {
            return HyphenationDialogFragment().apply {
                arguments = createArguments(selectedValue.ordinal - 1)
            }
        }
    }
}