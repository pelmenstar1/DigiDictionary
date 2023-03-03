package io.github.pelmenstar1.digiDict.ui.home.search

import android.content.Context
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import io.github.pelmenstar1.digiDict.common.FilteredArray
import io.github.pelmenstar1.digiDict.common.FilteredArrayDiffManager
import io.github.pelmenstar1.digiDict.common.android.TextBreakAndHyphenationInfo
import io.github.pelmenstar1.digiDict.common.getLazyValue
import io.github.pelmenstar1.digiDict.common.ui.RecyclerViewAdapterListUpdateCallback
import io.github.pelmenstar1.digiDict.data.ConciseRecordWithBadges
import io.github.pelmenstar1.digiDict.search.RecordSearchMetadataProvider
import io.github.pelmenstar1.digiDict.search.RecordSearchResult
import io.github.pelmenstar1.digiDict.ui.EntityWitIdFilteredArrayDiffCallback
import io.github.pelmenstar1.digiDict.ui.record.ConciseRecordWithBadgesViewHolder
import io.github.pelmenstar1.digiDict.ui.record.ConciseRecordWithBadgesViewHolderStaticInfo

class HomeSearchAdapter(
    onViewRecord: (id: Int) -> Unit
) : RecyclerView.Adapter<HomeSearchAdapter.ViewHolder>() {
    class ViewHolder(
        context: Context,
        onItemClickListener: View.OnClickListener,
        staticInfo: ConciseRecordWithBadgesViewHolderStaticInfo
    ) : ConciseRecordWithBadgesViewHolder(context, onItemClickListener, staticInfo) {
        fun bind(record: ConciseRecordWithBadges, style: HomeSearchItemStyle, hasDivider: Boolean) {
            bindExceptExpressionAndMeaning(record, hasDivider)
            setStyledExpressionAndMeaning(record, style)
        }

        fun setStyledExpressionAndMeaning(record: ConciseRecordWithBadges, style: HomeSearchItemStyle) {
            val context = container.context

            container.setExpressionAndMeaning(
                expr = HomeSearchStyledTextUtil.createExpressionText(record.expression, style),
                createMeaning = { HomeSearchStyledTextUtil.createMeaningText(context, record.meaning, style) }
            )
        }
    }

    private val onItemClickListener = ConciseRecordWithBadgesViewHolder.createOnItemClickListener(onViewRecord)
    private var staticInfo: ConciseRecordWithBadgesViewHolderStaticInfo? = null

    private var currentData: FilteredArray<out ConciseRecordWithBadges> = FilteredArray.empty()
    private val listUpdateCallback = RecyclerViewAdapterListUpdateCallback(this)

    private val diffManager = FilteredArrayDiffManager(conciseRecordDiffCallback)

    private var metadataProvider: RecordSearchMetadataProvider? = null
    private var breakAndHyphenationInfo: TextBreakAndHyphenationInfo? = null

    fun setTextBreakAndHyphenationInfo(info: TextBreakAndHyphenationInfo) {
        breakAndHyphenationInfo = info

        currentData.size.also {
            if (it > 0) {
                notifyItemRangeChanged(0, it, updateBreakAndHyphenationInfoPayload)
            }
        }
    }

    fun submitResult(result: RecordSearchResult) {
        val currentData = result.currentData
        val currentDataSize = currentData.size

        val previousData = result.previousData

        this.currentData = currentData
        metadataProvider = result.metadataProvider

        // calculateDifference is a quite expensive method, so it's better not to call when it's possible.
        if (previousData.size == 0) {
            if (currentDataSize > 0) {
                notifyItemRangeInserted(0, currentDataSize)
            }
        } else {
            val diffResult = diffManager.calculateDifference(previousData, currentData)

            diffResult.dispatchTo(listUpdateCallback)
        }

        if (currentDataSize > 0) {
            // On new search request, found ranges in items might be changed regardless of whether its position is changed or not.
            // So we should update it.
            notifyItemRangeChanged(0, currentDataSize, updateStyledExpressionAndMeaning)
        }
    }

    fun submitEmpty() {
        // Let the GC do its job. As the adapter is going to empty, onBindViewHolder, that requires metadataProvider to be non-null,
        // won't be called
        metadataProvider = null

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

        return ViewHolder(context, onItemClickListener, si).also {
            it.setTextBreakAndHyphenationInfoCompat(breakAndHyphenationInfo)
        }
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val data = currentData
        val record = data[position]
        val itemStyle = createItemStyle(record)

        // Don't show divider if the item is the last one.
        val hasDivider = position < data.size - 1

        holder.bind(record, itemStyle, hasDivider)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int, payloads: MutableList<Any>) {
        if (payloads.isEmpty()) {
            onBindViewHolder(holder, position)
        } else {
            when (payloads[0]) {
                updateBreakAndHyphenationInfoPayload -> {
                    holder.setTextBreakAndHyphenationInfoCompat(breakAndHyphenationInfo)
                }
                updateStyledExpressionAndMeaning -> {
                    val record = currentData[position]
                    val itemStyle = createItemStyle(record)

                    holder.setStyledExpressionAndMeaning(record, itemStyle)
                }
            }
        }
    }

    private fun createItemStyle(record: ConciseRecordWithBadges): HomeSearchItemStyle {
        val foundRanges = requireNotNull(metadataProvider).calculateFoundRanges(record)

        return HomeSearchItemStyle(foundRanges)
    }

    companion object {
        private val conciseRecordDiffCallback = EntityWitIdFilteredArrayDiffCallback<ConciseRecordWithBadges>()

        private val updateBreakAndHyphenationInfoPayload = Any()
        private val updateStyledExpressionAndMeaning = Any()
    }
}