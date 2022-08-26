package io.github.pelmenstar1.digiDict.ui.addEditRecord.badge

import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.textview.MaterialTextView
import io.github.pelmenstar1.digiDict.R
import io.github.pelmenstar1.digiDict.common.ui.setPaddingRes
import io.github.pelmenstar1.digiDict.common.ui.setTextAppearance
import io.github.pelmenstar1.digiDict.data.RecordBadgeInfo

class BadgeSelectorDialogAdapter(
    private val onItemClickListener: (RecordBadgeInfo) -> Unit
) : RecyclerView.Adapter<BadgeSelectorDialogAdapter.ViewHolder>() {
    private class Callback(
        private val old: List<RecordBadgeInfo>,
        private val new: List<RecordBadgeInfo>
    ) : DiffUtil.Callback() {
        override fun getOldListSize() = old.size
        override fun getNewListSize() = new.size

        override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
            return old[oldItemPosition].id == new[newItemPosition].id
        }

        override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
            return old[oldItemPosition] == new[newItemPosition]
        }
    }

    inner class ViewHolder(private val textView: TextView) : RecyclerView.ViewHolder(textView) {
        init {
            textView.setOnClickListener(containerOnItemClickListener)
        }

        fun bind(value: RecordBadgeInfo) {
            textView.tag = value
            textView.text = value.name
        }
    }

    private val containerOnItemClickListener = View.OnClickListener {
        val badge = ((it as TextView).tag as RecordBadgeInfo)

        onItemClickListener(badge)
    }

    private var elements = emptyList<RecordBadgeInfo>()

    fun submitData(elements: List<RecordBadgeInfo>) {
        val result = DiffUtil.calculateDiff(Callback(this.elements, elements))
        this.elements = elements

        result.dispatchUpdatesTo(this)
    }

    override fun getItemCount() = elements.size

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val textView = MaterialTextView(parent.context).apply {
            layoutParams = ITEM_LAYOUT_PARAMS

            setPaddingRes(R.dimen.badgeSelectorDialog_listElementPadding)
            setTextAppearance { BodyLarge }
        }

        return ViewHolder(textView)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(elements[position])
    }

    companion object {
        private val ITEM_LAYOUT_PARAMS = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )
    }
}