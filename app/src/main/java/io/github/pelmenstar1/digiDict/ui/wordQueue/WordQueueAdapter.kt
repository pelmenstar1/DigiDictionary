package io.github.pelmenstar1.digiDict.ui.wordQueue

import android.content.Context
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import io.github.pelmenstar1.digiDict.R
import io.github.pelmenstar1.digiDict.data.WordQueueEntry
import io.github.pelmenstar1.digiDict.ui.ArrayDiffCallback

class WordQueueAdapter(
    private val onAddMeaning: (entry: WordQueueEntry) -> Unit,
    private val onRemoveFromQueue: (entry: WordQueueEntry) -> Unit
) : RecyclerView.Adapter<WordQueueAdapter.ViewHolder>() {
    class ViewHolder(private val card: WordQueueCard) : RecyclerView.ViewHolder(card) {
        fun bind(value: WordQueueEntry) = card.bind(value)
    }

    private var items: Array<out WordQueueEntry> = emptyArray()

    fun submitItems(newItems: Array<out WordQueueEntry>) {
        val result = DiffUtil.calculateDiff(ArrayDiffCallback(items, newItems))
        items = newItems

        result.dispatchUpdatesTo(this)
    }

    fun submitEmpty() {
        val size = items.size

        if (size > 0) {
            items = emptyArray()
            notifyItemRangeRemoved(0, size)
        }
    }

    override fun getItemCount(): Int = items.size

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val context = parent.context

        val card = WordQueueCard(context).apply {
            layoutParams = createLayoutParams(context)

            setOnAddMeaningListener(onAddMeaning)
            setOnRemoveFromQueueListener(onRemoveFromQueue)
        }

        return ViewHolder(card)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(items[position])
    }

    companion object {
        private fun createLayoutParams(context: Context): LinearLayout.LayoutParams {
            val res = context.resources

            return LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                val horizontalMargin = res.getDimensionPixelOffset(R.dimen.wordQueueCard_horizontalMargin)

                bottomMargin = res.getDimensionPixelOffset(R.dimen.wordQueueCard_bottomMargin)

                leftMargin = horizontalMargin
                rightMargin = horizontalMargin
            }
        }
    }
}