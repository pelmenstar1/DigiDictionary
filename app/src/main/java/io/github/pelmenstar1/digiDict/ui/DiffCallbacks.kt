package io.github.pelmenstar1.digiDict.ui

import androidx.recyclerview.widget.DiffUtil
import io.github.pelmenstar1.digiDict.common.FilteredArrayDiffItemCallback
import io.github.pelmenstar1.digiDict.data.EntityWithPrimaryKeyId

class EntityWitIdFilteredArrayDiffCallback<in T : EntityWithPrimaryKeyId>: FilteredArrayDiffItemCallback<T> {
    override fun areContentsTheSame(a: T, b: T): Boolean {
        return a == b
    }

    override fun areItemsTheSame(a: T, b: T): Boolean {
        return a.id == b.id
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