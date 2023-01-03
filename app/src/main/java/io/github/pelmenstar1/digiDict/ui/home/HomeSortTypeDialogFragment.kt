package io.github.pelmenstar1.digiDict.ui.home

import io.github.pelmenstar1.digiDict.R
import io.github.pelmenstar1.digiDict.common.ui.constListDialog.SimpleTextConstantListDialogFragment
import io.github.pelmenstar1.digiDict.data.HomeSortType

class HomeSortTypeDialogFragment : SimpleTextConstantListDialogFragment<HomeSortType>() {
    override val stringArrayResource: Int
        get() = R.array.home_sortTypes

    override fun getValues(): Array<out HomeSortType> = HomeSortType.values()
}