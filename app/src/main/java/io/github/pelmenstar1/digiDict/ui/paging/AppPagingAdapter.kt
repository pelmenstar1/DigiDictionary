package io.github.pelmenstar1.digiDict.ui.paging

import android.content.Context
import android.os.Build
import android.text.TextPaint
import android.util.Log
import android.util.SparseArray
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import androidx.annotation.RequiresApi
import androidx.paging.PagingDataAdapter
import androidx.recyclerview.widget.RecyclerView
import io.github.pelmenstar1.digiDict.common.android.TextBreakAndHyphenationInfo
import io.github.pelmenstar1.digiDict.common.getOrAdd
import io.github.pelmenstar1.digiDict.common.ui.getTypedViewAt
import io.github.pelmenstar1.digiDict.ui.paging.*
import io.github.pelmenstar1.digiDict.ui.record.*

class AppPagingAdapter(
    onViewRecord: (id: Int) -> Unit
) : PagingDataAdapter<PageItem, AppPagingAdapter.ViewHolder>(PageItemDiffCallback) {
    inner class ViewHolder(private val container: ViewGroup) : RecyclerView.ViewHolder(container) {
        private var type = TYPE_NONE

        // TODO: Use simple array instead. We can treat uniqueId as index.
        private val views = SparseArray<View>(4)

        @Suppress("UNCHECKED_CAST")
        fun bind(item: PageItem?) {
            if (item != null) {
                val context = container.context

                // Record item should be handled specially
                // due to the need of accessing onRecordClickListener and precomputed text support.
                if (item is PageItem.Record) {
                    val staticInfo = getRecordStaticInfo(context)
                    val view = views.getOrAdd(RECORD_ITEM_UNIQUE_ID) {
                        ConciseRecordWithBadgesViewHolder.createRootContainer(context, staticInfo).also { container ->
                            // Assign initial breakStrategy and hyphenationFrequency values
                            // if they exist.
                            if (Build.VERSION.SDK_INT >= 23) {
                                textBreakAndHyphenationInfo?.let { info ->
                                    ConciseRecordWithBadgesViewHolder.bindTextBreakAndHyphenationInfo(container, info)
                                }
                            }
                        }
                    }

                    replaceViewIfTypeDiffers(RECORD_ITEM_UNIQUE_ID, view)

                    ConciseRecordWithBadgesViewHolder.bind(
                        view as RecordItemRootContainer,
                        item.record,
                        hasDivider = true,
                        item.precomputedValues,
                        onItemClickListener,
                        staticInfo
                    )
                } else {
                    val inflater = getInflater(item) as PageItemInflater<PageItem, Any>
                    val id = inflater.uniqueId
                    val staticInfo = itemStaticInfoArray.getOrAdd(id) { inflater.createStaticInfo(context) }
                    val view = views.getOrAdd(id) { inflater.createView(context, staticInfo) }

                    replaceViewIfTypeDiffers(id, view)

                    inflater.bind(view, item, staticInfo)
                }
            } else {
                type = TYPE_NONE
                container.removeAllViews()
            }
        }

        /**
         * Updates breakStrategy and hyphenationFrequency in record's view holder.
         *
         * [textBreakAndHyphenationInfo] must not be null.
         */
        @RequiresApi(23)
        fun updateTextBreakAndHyphenationInfo() {
            if (type == RECORD_ITEM_UNIQUE_ID) {
                val root = container.getTypedViewAt<RecordItemRootContainer>(0)

                ConciseRecordWithBadgesViewHolder.bindTextBreakAndHyphenationInfo(root, textBreakAndHyphenationInfo!!)
            }
        }

        private fun getInflater(item: PageItem): PageItemInflater<*, *> = when (item) {
            is PageItem.Record -> throw IllegalArgumentException("PageItem.Record should not be passed to getInflater()")
            is PageItem.DateMarker -> DateMarkerInflater
            is PageItem.EventMarker -> EventMarkerInflater
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

    // TODO: Use simple array instead. We can treat uniqueId as index.
    private val itemStaticInfoArray = SparseArray<Any>(4)

    private var textBreakAndHyphenationInfo: TextBreakAndHyphenationInfo? = null

    /**
     * Gets the [TextPaint] instance that copies those properties of [TextPaint] in all expression [TextView]'s
     * that affect the text measuring.
     *
     * The returned value shouldn't be mutated.
     */
    fun getExpressionTextPaintForMeasure(context: Context): TextPaint {
        return getRecordStaticInfo(context).expressionTextPaintForMeasure
    }

    /**
     * Gets the [TextPaint] instance that copies those properties of [TextPaint] in all meaning [TextView]'s
     * that affect the text measuring.
     *
     * The returned value shouldn't be mutated.
     */
    fun getMeaningTextPaintForMeasure(context: Context): TextPaint {
        return getRecordStaticInfo(context).meaningTextPaintForMeasure
    }

    @RequiresApi(23)
    fun setTextBreakAndHyphenationInfo(value: TextBreakAndHyphenationInfo) {
        textBreakAndHyphenationInfo = value

        notifyItemRangeChanged(0, itemCount, CHANGE_TEXT_BREAK_AND_HYPHENATION_INFO)
    }

    private fun getRecordStaticInfo(context: Context): ConciseRecordWithBadgesViewHolderStaticInfo {
        return itemStaticInfoArray.getOrAdd(RECORD_ITEM_UNIQUE_ID) {
            ConciseRecordWithBadgesViewHolderStaticInfo(context)
        } as ConciseRecordWithBadgesViewHolderStaticInfo
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val container = FrameLayout(parent.context).apply {
            layoutParams = CONTAINER_LAYOUT_PARAMS
        }

        return ViewHolder(container)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int, payloads: MutableList<Any>) {
        if (payloads.isEmpty()) {
            onBindViewHolder(holder, position)

            return
        }

        val payload = payloads[0]

        if (payload === CHANGE_TEXT_BREAK_AND_HYPHENATION_INFO) {
            if (Build.VERSION.SDK_INT >= 23) {
                if (textBreakAndHyphenationInfo != null) {
                    holder.updateTextBreakAndHyphenationInfo()
                }
            } else {
                Log.e(TAG, "Request to re-bind break strategy and hyphenation frequency but API level < 23")
            }
        } else {
            Log.e(TAG, "Failed to bind when payload is unknown (${payload})")
        }

        super.onBindViewHolder(holder, position, payloads)
    }

    companion object {
        private const val TAG = "AppPagingAdapter"

        private val CHANGE_TEXT_BREAK_AND_HYPHENATION_INFO = Any()

        private val CONTAINER_LAYOUT_PARAMS = LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT,
        )

        private const val TYPE_NONE = 0
        private const val RECORD_ITEM_UNIQUE_ID = 1
    }
}