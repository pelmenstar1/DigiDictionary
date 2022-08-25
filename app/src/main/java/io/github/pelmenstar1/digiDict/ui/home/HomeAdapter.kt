package io.github.pelmenstar1.digiDict.ui.home

import android.view.ViewGroup
import androidx.paging.PagingDataAdapter
import io.github.pelmenstar1.digiDict.data.ConciseRecordWithBadges
import io.github.pelmenstar1.digiDict.ui.EntityWithPrimaryKeyIdItemDiffCallback
import io.github.pelmenstar1.digiDict.ui.record.ConciseRecordWithBadgesViewHolder

class HomeAdapter(
    onViewRecord: (id: Int) -> Unit
) : PagingDataAdapter<ConciseRecordWithBadges, ConciseRecordWithBadgesViewHolder>(EntityWithPrimaryKeyIdItemDiffCallback()) {
    private val onItemClickListener = ConciseRecordWithBadgesViewHolder.createOnItemClickListener(onViewRecord)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ConciseRecordWithBadgesViewHolder {
        return ConciseRecordWithBadgesViewHolder(parent.context)
    }

    override fun onBindViewHolder(holder: ConciseRecordWithBadgesViewHolder, position: Int) {
        val record = getItem(position)

        holder.bind(record, onItemClickListener)
    }
}