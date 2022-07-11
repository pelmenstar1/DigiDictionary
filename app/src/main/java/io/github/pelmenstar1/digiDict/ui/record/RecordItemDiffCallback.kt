package io.github.pelmenstar1.digiDict.ui.record

import androidx.recyclerview.widget.DiffUtil
import io.github.pelmenstar1.digiDict.data.Record

object RecordItemDiffCallback : DiffUtil.ItemCallback<Record>() {
    override fun areItemsTheSame(oldItem: Record, newItem: Record): Boolean {
        return oldItem.id == newItem.id
    }

    override fun areContentsTheSame(oldItem: Record, newItem: Record): Boolean {
        return oldItem == newItem
    }
}