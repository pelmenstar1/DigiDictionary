package io.github.pelmenstar1.digiDict.ui.settings

import android.os.Bundle
import io.github.pelmenstar1.digiDict.R
import io.github.pelmenstar1.digiDict.common.android.HyphenationFrequency
import io.github.pelmenstar1.digiDict.common.ui.selectionDialogs.ChoicesProvider
import io.github.pelmenstar1.digiDict.common.ui.selectionDialogs.SingleSelectionDialogFragment

class HyphenationDialogFragment : SingleSelectionDialogFragment<HyphenationFrequency>() {
    override val choices: ChoicesProvider
        get() = stringArrayResource(R.array.settings_hyphenation_dialogChoices)

    override val choicesInfoRes: Int
        get() = R.array.settings_hyphenation_dialogChoicesInfo

    override val titleRes: Int
        get() = R.string.settings_hyphenation

    // UNSPECIFIED is not used, so +1
    override fun getValueByIndex(index: Int): HyphenationFrequency = HyphenationFrequency.fromOrdinal(index + 1)

    companion object {
        fun createArguments(selectedValue: HyphenationFrequency): Bundle {
            // UNSPECIFIED is not used, so -1
            return createArguments(selectedValue.ordinal - 1)
        }
    }
}