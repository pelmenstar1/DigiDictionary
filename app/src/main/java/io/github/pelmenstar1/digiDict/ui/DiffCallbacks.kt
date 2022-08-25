package io.github.pelmenstar1.digiDict.ui

import androidx.recyclerview.widget.DiffUtil
import io.github.pelmenstar1.digiDict.common.FilteredArray
import io.github.pelmenstar1.digiDict.data.EntityWithPrimaryKeyId

class EntityWithPrimaryKeyIdItemDiffCallback<T : EntityWithPrimaryKeyId> : DiffUtil.ItemCallback<T>() {
    override fun areItemsTheSame(oldItem: T, newItem: T): Boolean {
        return oldItem.id == newItem.id
    }

    override fun areContentsTheSame(oldItem: T, newItem: T): Boolean {
        return oldItem == newItem
    }
}

class FilteredArrayDiffCallback<out T : EntityWithPrimaryKeyId>(
    private val oldArray: FilteredArray<T>,
    private val newArray: FilteredArray<T>
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

class ArrayDiffCallback<out T : EntityWithPrimaryKeyId>(
    private val oldArray: Array<out T>,
    private val newArray: Array<out T>
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