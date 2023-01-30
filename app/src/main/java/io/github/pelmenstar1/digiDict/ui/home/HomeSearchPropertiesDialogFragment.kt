package io.github.pelmenstar1.digiDict.ui.home

import io.github.pelmenstar1.digiDict.R
import io.github.pelmenstar1.digiDict.common.mapToIntArray
import io.github.pelmenstar1.digiDict.common.ui.MultiSelectionDialogFragment
import io.github.pelmenstar1.digiDict.search.RecordSearchProperty

class HomeSearchPropertiesDialogFragment : MultiSelectionDialogFragment<RecordSearchProperty>() {
    override val choicesRes: Int
        get() = R.array.home_searchProperties

    override val titleRes: Int
        get() = R.string.home_searchPropertyDialog_title

    override val atLeastOneShouldBeSelected: Boolean
        get() = true

    override fun getValueByIndex(index: Int) = RecordSearchProperty.ofOrdinal(index)
    override fun createValueArray(size: Int) = arrayOfNulls<RecordSearchProperty>(size)

    companion object {
        fun create(selectedProperties: Array<out RecordSearchProperty>): HomeSearchPropertiesDialogFragment {
            return HomeSearchPropertiesDialogFragment().also { dialog ->
                val selectedIndices = selectedProperties.mapToIntArray { it.ordinal }

                dialog.arguments = createArguments(selectedIndices)
            }
        }
    }
}