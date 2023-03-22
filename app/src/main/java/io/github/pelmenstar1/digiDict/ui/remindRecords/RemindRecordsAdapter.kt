package io.github.pelmenstar1.digiDict.ui.remindRecords

import android.content.Context
import android.view.View
import android.view.ViewGroup
import androidx.annotation.RequiresApi
import androidx.recyclerview.widget.RecyclerView
import io.github.pelmenstar1.digiDict.common.FixedBitSet
import io.github.pelmenstar1.digiDict.common.android.TextBreakAndHyphenationInfo
import io.github.pelmenstar1.digiDict.common.getLazyValue
import io.github.pelmenstar1.digiDict.data.ConciseRecordWithBadges
import io.github.pelmenstar1.digiDict.ui.record.ConciseRecordWithBadgesViewHolder
import io.github.pelmenstar1.digiDict.ui.record.ConciseRecordWithBadgesViewHolderStaticInfo

class RemindRecordsAdapter : RecyclerView.Adapter<RemindRecordsAdapter.ViewHolder>() {
    class ViewHolder(
        context: Context,
        onItemClickListener: View.OnClickListener,
        staticInfo: ConciseRecordWithBadgesViewHolderStaticInfo
    ) : ConciseRecordWithBadgesViewHolder(context, onItemClickListener, staticInfo) {
        fun setRevealed(state: Boolean) {
            // Use INVISIBLE instead of GONE to reduce jumps on revealing.
            val visibility = if (state) View.VISIBLE else View.INVISIBLE

            container.meaningView.visibility = visibility
            container.scoreView.visibility = visibility
        }
    }

    private val onItemClickListener = View.OnClickListener {
        val record = it.tag as ConciseRecordWithBadges
        val index = items.indexOf(record)

        if (index >= 0) {
            reveal(index)
        }
    }

    private var items = emptyArray<ConciseRecordWithBadges>()
    private var staticInfo: ConciseRecordWithBadgesViewHolderStaticInfo? = null

    private var breakAndHyphenationInfo: TextBreakAndHyphenationInfo? = null
    private var defaultRevealState = false

    private var _revealedStates = FixedBitSet.EMPTY
    var revealedStates: FixedBitSet
        get() = _revealedStates
        set(value) {
            _revealedStates = value

            if (value.size != items.size) {
                throw IllegalStateException("revealedStates should be set after submitItems() and size should be equal to items size")
            }

            notifyAllItemsChanged(updateRevealStatePayload)
        }

    @RequiresApi(23)
    fun setBreakAndHyphenationInfo(value: TextBreakAndHyphenationInfo) {
        breakAndHyphenationInfo = value

        notifyAllItemsChanged(updateBreakStrategyAndHyphenationPayload)
    }

    fun setDefaultRevealState(value: Boolean) {
        if (defaultRevealState != value) {
            defaultRevealState = value

            notifyAllItemsChanged(updateRevealStatePayload)
        }
    }

    fun submitItems(newItems: Array<ConciseRecordWithBadges>) {
        val newSize = newItems.size
        val revStates = _revealedStates
        val defRevealState = defaultRevealState

        if (revStates.size != newItems.size) {
            _revealedStates = FixedBitSet(newSize).also {
                // Bit set is all zero by default, so that if defRevealState is false,
                // there's no sense to set it all zero again.
                if (defRevealState) {
                    it.setAll()
                }
            }
        } else {
            revStates.setAll(defRevealState)
        }

        val oldItemsLength = items.size
        items = newItems

        notifyItemsChanged(oldItemsLength, newSize)
    }

    private fun notifyItemsChanged(oldSize: Int, newSize: Int) {
        // Don't do complex diff here, because:
        // - old and new elements are almost always different.
        // - old and new sizes are almost always the same.
        when {
            oldSize == 0 && newSize > 0 -> {
                notifyItemRangeInserted(0, newSize)
            }
            oldSize != newSize -> {
                notifyItemRangeRemoved(0, oldSize)
                notifyItemRangeInserted(0, newSize)

                notifyItemRangeChanged(0, newSize)
            }
            // oldSize == newSize
            else -> {
                notifyItemRangeChanged(0, newSize)
            }
        }
    }

    private fun reveal(index: Int) {
        _revealedStates.set(index)

        notifyItemChanged(index, updateRevealStatePayload)
    }

    override fun getItemCount() = items.size

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
        holder.bind(
            record = items[position],
            hasDivider = position < items.size - 1,
            precomputedValues = null
        )

        holder.setRevealed(_revealedStates[position])
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int, payloads: MutableList<Any>) {
        if (payloads.isEmpty()) {
            onBindViewHolder(holder, position)
        } else {
            val payload = payloads[0]

            when {
                payload === updateRevealStatePayload -> {
                    holder.setRevealed(_revealedStates[position])
                }
                payload == updateBreakStrategyAndHyphenationPayload -> {
                    holder.setTextBreakAndHyphenationInfoCompat(breakAndHyphenationInfo)
                }
            }
        }
    }

    private fun notifyAllItemsChanged(payload: Any?) {
        val size = items.size
        if (size > 0) {
            notifyItemRangeChanged(0, size, payload)
        }
    }

    companion object {
        private val updateRevealStatePayload = Any()
        private val updateBreakStrategyAndHyphenationPayload = Any()
    }
}