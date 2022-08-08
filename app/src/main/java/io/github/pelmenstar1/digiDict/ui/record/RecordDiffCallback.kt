package io.github.pelmenstar1.digiDict.ui.record

import androidx.recyclerview.widget.DiffUtil
import io.github.pelmenstar1.digiDict.common.FilteredArray
import io.github.pelmenstar1.digiDict.data.Record

object RecordItemDiffCallback : DiffUtil.ItemCallback<Record>() {
    override fun areItemsTheSame(oldItem: Record, newItem: Record): Boolean {
        return oldItem.id == newItem.id
    }

    override fun areContentsTheSame(oldItem: Record, newItem: Record): Boolean {
        return oldItem == newItem
    }
}

class FilteredArrayRecordDiffCallback(
    private val oldArray: FilteredArray<Record>,
    private val newArray: FilteredArray<Record>
) : DiffUtil.Callback() {
    override fun getOldListSize() = oldArray.size
    override fun getNewListSize() = newArray.size

    override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
        return oldArray[oldItemPosition].id == newArray[newItemPosition].id
    }

    override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
        return oldArray[oldItemPosition] == newArray[newItemPosition]
    }
}