package io.github.pelmenstar1.digiDict.ui.home

import io.github.pelmenstar1.digiDict.R
import io.github.pelmenstar1.digiDict.common.ui.selectionDialogs.ChoicesProvider
import io.github.pelmenstar1.digiDict.common.ui.selectionDialogs.MultiSelectionDialogFragment
import io.github.pelmenstar1.digiDict.search.RecordSearchProperty
import io.github.pelmenstar1.digiDict.search.RecordSearchPropertySet

class HomeSearchPropertiesDialogFragment : MultiSelectionDialogFragment<RecordSearchProperty>() {
    override val choices: ChoicesProvider
        get() = stringArrayResource(R.array.home_searchProperties)

    override val titleRes: Int
        get() = R.string.home_searchPropertyDialog_title

    override val atLeastOneShouldBeSelected: Boolean
        get() = true

    override fun getValueByIndex(index: Int) = RecordSearchProperty.ofOrdinal(index)
    override fun createValueArray(size: Int) = arrayOfNulls<RecordSearchProperty>(size)

    companion object {
        fun create(selectedProperties: RecordSearchPropertySet): HomeSearchPropertiesDialogFragment {
            return HomeSearchPropertiesDialogFragment().also { dialog ->
                val selectedIndices = selectedProperties.toOrdinalArray()

                dialog.arguments = createArguments(selectedIndices)
            }
        }
    }
}