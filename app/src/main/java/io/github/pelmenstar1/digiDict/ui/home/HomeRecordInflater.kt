package io.github.pelmenstar1.digiDict.ui.home

import android.content.Context
import android.view.View
import io.github.pelmenstar1.digiDict.ui.record.ConciseRecordWithBadgesViewHolder
import io.github.pelmenstar1.digiDict.ui.record.ConciseRecordWithBadgesViewHolderStaticInfo
import io.github.pelmenstar1.digiDict.ui.record.RecordItemRootContainer

object HomeRecordInflater : HomePageItemInflater<HomePageItem.Record, ConciseRecordWithBadgesViewHolderStaticInfo> {
    override val uniqueId: Int
        get() = 1

    override fun createStaticInfo(context: Context): ConciseRecordWithBadgesViewHolderStaticInfo {
        return ConciseRecordWithBadgesViewHolderStaticInfo(context)
    }

    override fun createView(context: Context, staticInfo: ConciseRecordWithBadgesViewHolderStaticInfo): View {
        return ConciseRecordWithBadgesViewHolder.createRootContainer(context, staticInfo)
    }

    override fun bind(
        view: View,
        item: HomePageItem.Record,
        args: HomePageItemInflaterArgs,
        staticInfo: ConciseRecordWithBadgesViewHolderStaticInfo
    ) {
        ConciseRecordWithBadgesViewHolder.bind(
            view as RecordItemRootContainer,
            item.record,
            hasDivider = true,
            onContainerClickListener = args.onRecordClickListener,
            staticInfo
        )
    }
}