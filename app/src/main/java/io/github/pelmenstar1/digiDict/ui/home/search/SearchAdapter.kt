package io.github.pelmenstar1.digiDict.ui.home.search

import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import io.github.pelmenstar1.digiDict.common.FilteredArray
import io.github.pelmenstar1.digiDict.common.getLazyValue
import io.github.pelmenstar1.digiDict.common.ui.AsyncDataDiffer
import io.github.pelmenstar1.digiDict.data.ConciseRecordWithBadges
import io.github.pelmenstar1.digiDict.ui.FilteredArrayDiffCallback
import io.github.pelmenstar1.digiDict.ui.record.ConciseRecordWithBadgesViewHolder
import io.github.pelmenstar1.digiDict.ui.record.ConciseRecordWithBadgesViewHolderStaticInfo
import kotlinx.coroutines.CoroutineScope

class SearchAdapter(
    differScope: CoroutineScope,
    onViewRecord: (id: Int) -> Unit
) : RecyclerView.Adapter<ConciseRecordWithBadgesViewHolder>() {
    private val onItemClickListener = ConciseRecordWithBadgesViewHolder.createOnItemClickListener(onViewRecord)
    private var staticInfo: ConciseRecordWithBadgesViewHolderStaticInfo? = null

    private val asyncDiffer = AsyncDataDiffer(
        adapter = this,
        scope = differScope,
        emptyData = FilteredArray.empty<ConciseRecordWithBadges>()
    ) { oldData, newData ->
        FilteredArrayDiffCallback(oldData, newData)
    }

    fun submitData(newData: FilteredArray<ConciseRecordWithBadges>) {
        asyncDiffer.submit(newData)
    }

    fun submitEmpty() {
        asyncDiffer.submitEmpty()
    }

    override fun getItemCount() = asyncDiffer.currentData.size

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
        val currentData = asyncDiffer.currentData
        val record = currentData[position]

        // Don't show divider if the item is the last one
        holder.bind(record, hasDivider = position < currentData.size - 1, onItemClickListener)
    }
}