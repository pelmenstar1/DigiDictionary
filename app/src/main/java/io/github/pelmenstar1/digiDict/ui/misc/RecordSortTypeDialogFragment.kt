package io.github.pelmenstar1.digiDict.ui.misc

import io.github.pelmenstar1.digiDict.R
import io.github.pelmenstar1.digiDict.common.ui.selectionDialogs.ChoicesProvider
import io.github.pelmenstar1.digiDict.common.ui.selectionDialogs.SingleSelectionDialogFragment
import io.github.pelmenstar1.digiDict.data.RecordSortType

class RecordSortTypeDialogFragment : SingleSelectionDialogFragment<RecordSortType>() {
    override val choices: ChoicesProvider
        get() = stringArrayResource(R.array.recordSortTypes)

    override val titleRes: Int
        get() = R.string.recordSortTypeDialog_title


    override fun getValueByIndex(index: Int) = RecordSortType.fromOrdinal(index)

    companion object {
        fun create(selectedIndex: Int): RecordSortTypeDialogFragment {
            return RecordSortTypeDialogFragment().apply {
                arguments = createArguments(selectedIndex)
            }
        }

        fun create(selectedValue: RecordSortType): RecordSortTypeDialogFragment {
            return create(selectedValue.ordinal)
        }
    }
}