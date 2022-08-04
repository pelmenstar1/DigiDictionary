package io.github.pelmenstar1.digiDict.ui.remindRecords

import android.content.Context
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import io.github.pelmenstar1.digiDict.EmptyArray
import io.github.pelmenstar1.digiDict.data.Record
import io.github.pelmenstar1.digiDict.ui.record.ArrayRecordDiffCallback
import io.github.pelmenstar1.digiDict.ui.record.RecordViewHolder
import io.github.pelmenstar1.digiDict.utils.FixedBitSet

class RemindRecordsAdapter : RecyclerView.Adapter<RemindRecordsAdapter.ViewHolder>() {
    class ViewHolder(context: Context) : RecordViewHolder(context) {
        init {
            setRevealed(false)
        }

        fun setRevealed(state: Boolean) {
            // Use INVISIBLE instead of GONE to reduce jumps on revealing.
            val visibility = if (state) View.VISIBLE else View.INVISIBLE

            meaningView.visibility = visibility
            scoreView.visibility = visibility
        }
    }

    private val onContainerClickListener = View.OnClickListener {
        val record = it.tag as Record
        val index = items.indexOf(record)

        if (index >= 0) {
            reveal(index)
        }
    }

    private var items = EmptyArray.RECORD

    var revealedStates: FixedBitSet = FixedBitSet.EMPTY
        set(value) {
            field = value

            if (value.size != items.size) {
                throw IllegalStateException("revealedStates should be set after submitItems() and size should be equal to items size")
            }

            value.iterateSetBits { index ->
                reveal(index)
            }
        }

    fun submitItems(newItems: Array<Record>) {
        val diffResult = DiffUtil.calculateDiff(ArrayRecordDiffCallback(items, newItems))
        items = newItems

        diffResult.dispatchUpdatesTo(this)
    }

    private fun reveal(index: Int) {
        notifyItemChanged(index, revealPayload)
    }

    fun concealAll() {
        notifyItemRangeChanged(0, items.size, concealPayload)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(parent.context)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(items[position], onContainerClickListener)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int, payloads: MutableList<Any>) {
        if (payloads.isEmpty()) {
            onBindViewHolder(holder, position)
        } else {
            val payload = payloads[0]

            when {
                payload === revealPayload -> holder.setRevealed(true)
                payload === concealPayload -> holder.setRevealed(false)
            }
        }
    }

    override fun getItemCount() = items.size

    companion object {
        private val revealPayload = Any()
        private val concealPayload = Any()
    }
}