package io.github.pelmenstar1.digiDict.ui.paging

import android.content.Context
import android.os.Build
import android.text.TextPaint
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import androidx.annotation.RequiresApi
import androidx.paging.PagingDataAdapter
import androidx.recyclerview.widget.RecyclerView
import io.github.pelmenstar1.digiDict.common.android.TextBreakAndHyphenationInfo
import io.github.pelmenstar1.digiDict.common.getOrCreateAndSet
import io.github.pelmenstar1.digiDict.ui.paging.*
import io.github.pelmenstar1.digiDict.ui.record.*

class AppPagingAdapter(
    onViewRecord: (id: Int) -> Unit
) : PagingDataAdapter<PageItem, RecyclerView.ViewHolder>(PageItemDiffCallback) {
    inner class RecordViewHolder(private val root: RecordItemRootContainer) : RecyclerView.ViewHolder(root) {
        init {
            // Assign initial breakStrategy and hyphenationFrequency values
            // if they exist.
            if (Build.VERSION.SDK_INT >= 23) {
                textBreakAndHyphenationInfo?.let { info ->
                    ConciseRecordWithBadgesViewHolder.bindTextBreakAndHyphenationInfo(root, info)
                }
            }
        }

        fun bind(item: PageItem.Record) {
            val context = root.context
            val staticInfo = getRecordStaticInfo(context)

            ConciseRecordWithBadgesViewHolder.bind(
                root,
                item.record,
                hasDivider = !item.isBeforeDateMarker,
                item.precomputedValues,
                onItemClickListener,
                staticInfo
            )
        }


        /**
         * Updates breakStrategy and hyphenationFrequency in record's view holder.
         *
         * [textBreakAndHyphenationInfo] must not be null.
         */
        @RequiresApi(23)
        fun updateTextBreakAndHyphenationInfo() {
            ConciseRecordWithBadgesViewHolder.bindTextBreakAndHyphenationInfo(root, textBreakAndHyphenationInfo!!)
        }
    }

    inner class NonRecordViewHolder(
        viewContainer: ViewGroup,
        private val type: Int
    ) : RecyclerView.ViewHolder(viewContainer) {
        private val view = viewContainer.getChildAt(0)
        private val inflater = getNonRecordInflater(type)

        fun bind(item: PageItem) {
            val context = view.context
            val staticInfo = getNonRecordStaticInfo(context, inflater, type)

            inflater.bind(view, item, staticInfo)
        }
    }

    private val onItemClickListener = ConciseRecordWithBadgesViewHolder.createOnItemClickListener(onViewRecord)

    // We have only 3 different page items.
    private val itemStaticInfoArray = arrayOfNulls<Any>(3)

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

        itemCount.also {
            if (it > 0) {
                notifyItemRangeChanged(0, it, updateTextBreakAndHyphenationInfoPayload)
            }
        }

    }

    override fun getItemViewType(position: Int): Int {
        return getItemType(getItemNotNull(position))
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val context = parent.context

        return when (viewType) {
            TYPE_RECORD -> {
                val staticInfo = getRecordStaticInfo(context)
                val root = ConciseRecordWithBadgesViewHolder.createRootContainer(context, staticInfo)

                RecordViewHolder(root)
            }
            TYPE_DATE_MARKER, TYPE_EVENT_MARKER -> {
                val inflater = getNonRecordInflater(viewType)
                val staticInfo = getNonRecordStaticInfo(context, inflater, viewType)
                val view = inflater.createView(context, staticInfo)

                NonRecordViewHolder(createNonRecordItemContainer(context, view), viewType)
            }
            else -> throw IllegalArgumentException("Invalid viewType")
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val item = getItemNotNull(position)

        when (holder) {
            is RecordViewHolder -> holder.bind(item as PageItem.Record)
            is NonRecordViewHolder -> holder.bind(item)
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int, payloads: MutableList<Any>) {
        if (payloads.isEmpty()) {
            onBindViewHolder(holder, position)

            return
        }

        val payload = payloads[0]

        if (payload === updateTextBreakAndHyphenationInfoPayload) {
            if (Build.VERSION.SDK_INT >= 23) {
                if (holder is RecordViewHolder && textBreakAndHyphenationInfo != null) {
                    holder.updateTextBreakAndHyphenationInfo()
                }
            } else {
                Log.e(TAG, "Request to re-bind break strategy and hyphenation frequency but API level < 23")
            }
        } else {
            Log.e(TAG, "Failed to bind when payload is unknown (${payload})")
        }
    }

    private fun createNonRecordItemContainer(context: Context, itemView: View): ViewGroup {
        return FrameLayout(context).apply {
            layoutParams = NON_RECORD_CONTAINER_LAYOUT_PARAMS

            addView(itemView)
        }
    }

    private fun getRecordStaticInfo(context: Context): ConciseRecordWithBadgesViewHolderStaticInfo {
        return itemStaticInfoArray.getOrCreateAndSet(TYPE_RECORD) {
            ConciseRecordWithBadgesViewHolderStaticInfo(context)
        } as ConciseRecordWithBadgesViewHolderStaticInfo
    }

    private fun getNonRecordStaticInfo(context: Context, inflater: PageItemInflater<*, Any>, type: Int): Any {
        return itemStaticInfoArray.getOrCreateAndSet(type) {
            inflater.createStaticInfo(context)
        }
    }

    private fun getItemNotNull(position: Int): PageItem {
        return requireNotNull(getItem(position)) { "Placeholders are forbidden" }
    }

    companion object {
        private const val TAG = "AppPagingAdapter"

        private val updateTextBreakAndHyphenationInfoPayload = Any()

        private val NON_RECORD_CONTAINER_LAYOUT_PARAMS = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT,
        )

        private const val TYPE_RECORD = 0
        private const val TYPE_DATE_MARKER = 1
        private const val TYPE_EVENT_MARKER = 2

        internal fun getItemType(item: PageItem): Int = when (item) {
            is PageItem.Record -> TYPE_RECORD
            is PageItem.DateMarker -> TYPE_DATE_MARKER
            is PageItem.EventMarker -> TYPE_EVENT_MARKER
        }

        @Suppress("UNCHECKED_CAST")
        internal fun getNonRecordInflater(type: Int): PageItemInflater<PageItem, Any> = when (type) {
            TYPE_DATE_MARKER -> DateMarkerInflater
            TYPE_EVENT_MARKER -> EventMarkerInflater
            else -> throw IllegalArgumentException("Invalid ordinal")
        } as PageItemInflater<PageItem, Any>
    }
}