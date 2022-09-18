package io.github.pelmenstar1.digiDict.ui.home

import android.view.ViewGroup
import androidx.paging.PagingDataAdapter
import io.github.pelmenstar1.digiDict.common.getLazyValue
import io.github.pelmenstar1.digiDict.data.ConciseRecordWithBadges
import io.github.pelmenstar1.digiDict.ui.EntityWithPrimaryKeyIdItemDiffCallback
import io.github.pelmenstar1.digiDict.ui.record.ConciseRecordWithBadgesViewHolder
import io.github.pelmenstar1.digiDict.ui.record.ConciseRecordWithBadgesViewHolderStaticInfo

class HomeAdapter(
    onViewRecord: (id: Int) -> Unit
) : PagingDataAdapter<ConciseRecordWithBadges, ConciseRecordWithBadgesViewHolder>(EntityWithPrimaryKeyIdItemDiffCallback()) {
    private val onItemClickListener = ConciseRecordWithBadgesViewHolder.createOnItemClickListener(onViewRecord)
    private var staticInfo: ConciseRecordWithBadgesViewHolderStaticInfo? = null

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ConciseRecordWithBadgesViewHolder {
        val context = parent.context
        val si = getLazyValue(
            staticInfo,
            { ConciseRecordWithBadgesViewHolderStaticInfo(context) },
            { staticInfo = it }
        )

        return ConciseRecordWithBadgesViewHolder(context, si)
    }

    override fun onBindViewHolder(holder: ConciseRecordWithBadgesViewHolder, position: Int) {
        val record = getItem(position)

        holder.bind(record, onItemClickListener)
    }
}