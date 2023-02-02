package io.github.pelmenstar1.digiDict.ui.home

import io.github.pelmenstar1.digiDict.R
import io.github.pelmenstar1.digiDict.common.ui.SingleSelectionDialogFragment
import io.github.pelmenstar1.digiDict.data.RecordSortType

class HomeSortTypeDialogFragment : SingleSelectionDialogFragment<RecordSortType>() {
    override val choicesRes: Int
        get() = R.array.home_sortTypes

    override val titleRes: Int
        get() = R.string.home_sortTypeDialog_title


    override fun getValueByIndex(index: Int) = RecordSortType.fromOrdinal(index)

    companion object {
        fun create(selectedIndex: Int): HomeSortTypeDialogFragment {
            return HomeSortTypeDialogFragment().apply {
                arguments = createArguments(selectedIndex)
            }
        }

        fun create(selectedValue: RecordSortType): HomeSortTypeDialogFragment {
            return create(selectedValue.ordinal)
        }
    }
}