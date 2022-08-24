package io.github.pelmenstar1.digiDict.ui.remindRecords

import android.content.Context
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import io.github.pelmenstar1.digiDict.common.FixedBitSet
import io.github.pelmenstar1.digiDict.data.ConciseRecordWithBadges
import io.github.pelmenstar1.digiDict.ui.record.ConciseRecordWithBadgesViewHolder

class RemindRecordsAdapter : RecyclerView.Adapter<RemindRecordsAdapter.ViewHolder>() {
    class ViewHolder(context: Context) : ConciseRecordWithBadgesViewHolder(context) {
        fun setRevealed(state: Boolean) {
            // Use INVISIBLE instead of GONE to reduce jumps on revealing.
            val visibility = if (state) View.VISIBLE else View.INVISIBLE

            meaningView.visibility = visibility
            scoreView.visibility = visibility
        }
    }

    private val onContainerClickListener = View.OnClickListener {
        val record = it.tag as ConciseRecordWithBadges
        val index = items.indexOf(record)

        if (index >= 0) {
            reveal(index)
        }
    }

    private var items = emptyArray<ConciseRecordWithBadges>()

    private var _revealedStates = FixedBitSet.EMPTY
    var revealedStates: FixedBitSet
        get() = _revealedStates
        set(value) {
            _revealedStates = value

            if (value.size != items.size) {
                throw IllegalStateException("revealedStates should be set after submitItems() and size should be equal to items size")
            }

            notifyItemRangeChanged(0, items.size, updateRevealStatePayload)
        }

    fun submitItems(newItems: Array<ConciseRecordWithBadges>, defaultRevealState: Boolean) {
        val newSize = newItems.size
        val revStates = _revealedStates

        if (revStates.size != newItems.size) {
            _revealedStates = FixedBitSet(newSize).also {
                if (defaultRevealState) {
                    it.setAll()
                }
            }
        } else {
            revStates.setAll(defaultRevealState)
        }

        val oldItems = items
        val oldSize = oldItems.size

        items = newItems

        when {
            oldSize == 0 && newSize >= 0 -> {
                notifyItemRangeInserted(0, newSize)
            }
            // Almost impossible situation but should be handled.
            oldSize != newSize -> {
                notifyItemRangeRemoved(0, oldSize)
                notifyItemRangeInserted(0, newSize)

                notifyItemRangeChanged(0, newSize)
            }
            else -> {
                notifyItemRangeChanged(0, newSize)
            }
        }
    }

    private fun reveal(index: Int) {
        revealedStates.set(index)

        notifyItemChanged(index, updateRevealStatePayload)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(parent.context)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(items[position], onContainerClickListener)
        holder.setRevealed(revealedStates[position])
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int, payloads: MutableList<Any>) {
        if (payloads.isEmpty()) {
            onBindViewHolder(holder, position)
        } else {
            val payload = payloads[0]

            if (payload === updateRevealStatePayload) {
                holder.setRevealed(_revealedStates[position])
            }
        }
    }

    override fun getItemCount() = items.size

    companion object {
        private val updateRevealStatePayload = Any()
    }
}