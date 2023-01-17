package io.github.pelmenstar1.digiDict.ui.home.search

import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import io.github.pelmenstar1.digiDict.common.FilteredArray
import io.github.pelmenstar1.digiDict.common.FilteredArrayDiffManager
import io.github.pelmenstar1.digiDict.common.getLazyValue
import io.github.pelmenstar1.digiDict.common.ui.RecyclerViewAdapterListUpdateCallback
import io.github.pelmenstar1.digiDict.data.ConciseRecordWithBadges
import io.github.pelmenstar1.digiDict.ui.EntityWitIdFilteredArrayDiffCallback
import io.github.pelmenstar1.digiDict.ui.record.ConciseRecordWithBadgesViewHolder
import io.github.pelmenstar1.digiDict.ui.record.ConciseRecordWithBadgesViewHolderStaticInfo

class HomeSearchAdapter(
    onViewRecord: (id: Int) -> Unit
) : RecyclerView.Adapter<ConciseRecordWithBadgesViewHolder>() {
    private val onItemClickListener = ConciseRecordWithBadgesViewHolder.createOnItemClickListener(onViewRecord)
    private var staticInfo: ConciseRecordWithBadgesViewHolderStaticInfo? = null

    private var currentData: FilteredArray<out ConciseRecordWithBadges> = FilteredArray.empty()
    private val listUpdateCallback = RecyclerViewAdapterListUpdateCallback(this)

    private val diffManager = FilteredArrayDiffManager(conciseRecordDiffCallback)

    fun submitResult(result: HomeSearchResult) {
        val currentData = result.currentData
        val previousData = result.previousData

        this.currentData = currentData

        // calculateDifference is a quite expensive method, so it's better not to call when it's possible.
        if (previousData.size == 0) {
            currentData.size.let {
                if (it > 0) {
                    notifyItemRangeInserted(0, it)
                }
            }
        } else {
            val diffResult = diffManager.calculateDifference(previousData, currentData)

            diffResult.dispatchTo(listUpdateCallback)
        }
    }

    fun submitEmpty() {
        currentData.size.let {
            if(it > 0) {
                currentData = FilteredArray.empty()

                notifyItemRangeRemoved(0, it)
            }
        }
    }

    override fun getItemCount() = currentData.size

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
        val record = currentData[position]

        // Don't show divider if the item is the last one
        holder.bind(record, hasDivider = position < currentData.size - 1, onItemClickListener)
    }

    companion object {
        private val conciseRecordDiffCallback = EntityWitIdFilteredArrayDiffCallback<ConciseRecordWithBadges>()
    }
}