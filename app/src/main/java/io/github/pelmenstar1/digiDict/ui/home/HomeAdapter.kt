package io.github.pelmenstar1.digiDict.ui.home

import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import androidx.paging.PagingDataAdapter
import androidx.recyclerview.widget.RecyclerView
import io.github.pelmenstar1.digiDict.common.getLazyValue
import io.github.pelmenstar1.digiDict.ui.record.ConciseRecordWithBadgesViewHolder
import io.github.pelmenstar1.digiDict.ui.record.ConciseRecordWithBadgesViewHolderStaticInfo

class HomeAdapter(
    onViewRecord: (id: Int) -> Unit
) : PagingDataAdapter<HomePageItem, HomeAdapter.HomeItemViewHolder>(HomePageItemDiffCallback) {
    inner class HomeItemViewHolder(private val container: ViewGroup) : RecyclerView.ViewHolder(container) {
        private var type = TYPE_NONE

        private var recordRootContainer: ViewGroup? = null
        private var dateMarkerTextView: TextView? = null

        fun bind(item: HomePageItem?) {
            val context = container.context

            when (item) {
                is HomePageItem.Record -> {
                    val staticInfo = getLazyValue(
                        recordStaticInfo,
                        { ConciseRecordWithBadgesViewHolderStaticInfo(context) },
                        { recordStaticInfo = it }
                    )

                    val root = getLazyValue(
                        recordRootContainer,
                        { ConciseRecordWithBadgesViewHolder.createRootContainer(context, staticInfo) },
                        { recordRootContainer = it }
                    )

                    replaceViewIfTypeDiffers(TYPE_RECORD, root)

                    ConciseRecordWithBadgesViewHolder.bind(root, item.value, onItemClickListener, staticInfo)
                }

                is HomePageItem.DateMarker -> {
                    val staticInfo = getLazyValue(
                        dateMarkerStaticInfo,
                        { HomeDateMarkerHelper.StaticInfo(context) },
                        { dateMarkerStaticInfo = it }
                    )

                    val textView = getLazyValue(
                        dateMarkerTextView,
                        { HomeDateMarkerHelper.createView(context, staticInfo) },
                        { dateMarkerTextView = it }
                    )

                    replaceViewIfTypeDiffers(TYPE_DATE_MARKER, textView)

                    HomeDateMarkerHelper.bind(textView, item.epochDay, staticInfo)
                }
                null -> {
                    type = TYPE_NONE
                    container.removeAllViews()
                }
            }
        }

        private fun replaceViewIfTypeDiffers(expectedType: Int, view: View) {
            if (type != expectedType) {
                container.removeAllViews()
                container.addView(view)

                type = expectedType
            }
        }
    }

    private val onItemClickListener = ConciseRecordWithBadgesViewHolder.createOnItemClickListener(onViewRecord)

    private var recordStaticInfo: ConciseRecordWithBadgesViewHolderStaticInfo? = null
    private var dateMarkerStaticInfo: HomeDateMarkerHelper.StaticInfo? = null

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HomeItemViewHolder {
        val context = parent.context
        val container = FrameLayout(context).apply {
            layoutParams = CONTAINER_LAYOUT_PARAMS
        }

        return HomeItemViewHolder(container)
    }

    override fun onBindViewHolder(holder: HomeItemViewHolder, position: Int) {
        val item = getItem(position)

        holder.bind(item)
    }

    companion object {
        private val CONTAINER_LAYOUT_PARAMS = LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT,
        )

        private const val TYPE_NONE = 0
        private const val TYPE_RECORD = 1
        private const val TYPE_DATE_MARKER = 2
    }
}