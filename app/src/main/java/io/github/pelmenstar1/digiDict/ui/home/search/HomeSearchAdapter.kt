package io.github.pelmenstar1.digiDict.ui.home.search

import android.content.Context
import android.view.View.OnClickListener
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import io.github.pelmenstar1.digiDict.common.FilteredArray
import io.github.pelmenstar1.digiDict.common.FilteredArrayDiffManager
import io.github.pelmenstar1.digiDict.common.getLazyValue
import io.github.pelmenstar1.digiDict.common.ui.RecyclerViewAdapterListUpdateCallback
import io.github.pelmenstar1.digiDict.data.ConciseRecordWithBadges
import io.github.pelmenstar1.digiDict.search.RecordSearchMetadataProvider
import io.github.pelmenstar1.digiDict.search.RecordSearchResult
import io.github.pelmenstar1.digiDict.ui.EntityWitIdFilteredArrayDiffCallback
import io.github.pelmenstar1.digiDict.ui.record.ConciseRecordWithBadgesViewHolder
import io.github.pelmenstar1.digiDict.ui.record.ConciseRecordWithBadgesViewHolderStaticInfo

class HomeSearchAdapter(
    onViewRecord: (id: Int) -> Unit,
    private val metadataProvider: RecordSearchMetadataProvider
) : RecyclerView.Adapter<HomeSearchAdapter.ViewHolder>() {
    class ViewHolder(
        context: Context,
        staticInfo: ConciseRecordWithBadgesViewHolderStaticInfo
    ) : ConciseRecordWithBadgesViewHolder(context, staticInfo) {
        fun bind(
            record: ConciseRecordWithBadges,
            style: HomeSearchItemStyle,
            hasDivider: Boolean,
            onContainerClickListener: OnClickListener
        ) {
            container.hasDivider = hasDivider

            val context = container.context

            container.tag = record
            container.setOnClickListener(onContainerClickListener)

            expressionView.text = HomeSearchStyledTextUtil.createExpressionText(record.expression, style)
            meaningView.text = HomeSearchStyledTextUtil.createMeaningText(context, record.meaning, style)

            setScore(scoreView, record.score, staticInfo)
            setBadges(container, record.badges, staticInfo)
        }
    }

    private val onItemClickListener = ConciseRecordWithBadgesViewHolder.createOnItemClickListener(onViewRecord)
    private var staticInfo: ConciseRecordWithBadgesViewHolderStaticInfo? = null

    private var currentData: FilteredArray<out ConciseRecordWithBadges> = FilteredArray.empty()
    private val listUpdateCallback = RecyclerViewAdapterListUpdateCallback(this)

    private val diffManager = FilteredArrayDiffManager(conciseRecordDiffCallback)

    fun submitResult(result: RecordSearchResult) {
        metadataProvider.onQueryChanged(result.query)

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
            if (it > 0) {
                currentData = FilteredArray.empty()

                notifyItemRangeRemoved(0, it)
            }
        }
    }

    override fun getItemCount() = currentData.size

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val context = parent.context
        val si = getLazyValue(
            staticInfo,
            { ConciseRecordWithBadgesViewHolderStaticInfo(context) },
            { staticInfo = it }
        )

        return ViewHolder(context, si)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val record = currentData[position]
        val itemStyle = createItemStyle(record)

        // Don't show divider if the item is the last one
        holder.bind(record, itemStyle, hasDivider = position < currentData.size - 1, onItemClickListener)
    }

    private fun createItemStyle(record: ConciseRecordWithBadges): HomeSearchItemStyle {
        val foundRanges = metadataProvider.calculateFoundRanges(record)

        return HomeSearchItemStyle(foundRanges)
    }

    companion object {
        private val conciseRecordDiffCallback = EntityWitIdFilteredArrayDiffCallback<ConciseRecordWithBadges>()
    }
}