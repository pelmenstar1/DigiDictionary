package io.github.pelmenstar1.digiDict.ui.home

import android.view.View
import android.view.ViewGroup
import androidx.paging.PagingDataAdapter
import io.github.pelmenstar1.digiDict.data.Record
import io.github.pelmenstar1.digiDict.ui.record.RecordItemDiffCallback
import io.github.pelmenstar1.digiDict.ui.record.RecordViewHolder

class HomeAdapter(
    onViewRecord: (id: Int) -> Unit
) : PagingDataAdapter<Record, RecordViewHolder>(RecordItemDiffCallback) {
    private val onItemClickListener = RecordViewHolder.createOnItemClickListener(onViewRecord)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecordViewHolder {
        return RecordViewHolder(parent.context)
    }

    override fun onBindViewHolder(holder: RecordViewHolder, position: Int) {
        val record = getItem(position)

        holder.bind(record, onItemClickListener)
    }
}