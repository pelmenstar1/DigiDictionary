package io.github.pelmenstar1.digiDict.ui.home

import io.github.pelmenstar1.digiDict.R
import io.github.pelmenstar1.digiDict.common.ui.SingleSelectionDialogFragment
import io.github.pelmenstar1.digiDict.data.HomeSortType

class HomeSortTypeDialogFragment : SingleSelectionDialogFragment<HomeSortType>() {
    override val choicesRes: Int
        get() = R.array.home_sortTypes

    override val titleRes: Int
        get() = R.string.home_sortTypeDialog_title


    override fun getValueByIndex(index: Int) = HomeSortType.fromOrdinal(index)

    companion object {
        fun create(selectedIndex: Int): HomeSortTypeDialogFragment {
            return HomeSortTypeDialogFragment().apply {
                arguments = createArguments(selectedIndex)
            }
        }

        fun create(selectedValue: HomeSortType): HomeSortTypeDialogFragment {
            return create(selectedValue.ordinal)
        }
    }
}