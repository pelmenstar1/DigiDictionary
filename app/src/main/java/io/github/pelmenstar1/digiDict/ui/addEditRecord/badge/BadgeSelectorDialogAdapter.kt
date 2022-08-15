package io.github.pelmenstar1.digiDict.ui.addEditRecord.badge

import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.view.setPadding
import androidx.core.widget.TextViewCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.textview.MaterialTextView
import io.github.pelmenstar1.digiDict.R
import io.github.pelmenstar1.digiDict.common.FilteredArray

class BadgeSelectorDialogAdapter(
    private val onItemClickListener: (String) -> Unit
) : RecyclerView.Adapter<BadgeSelectorDialogAdapter.ViewHolder>() {
    private class Callback(
        private val old: FilteredArray<String>,
        private val new: FilteredArray<String>
    ) : DiffUtil.Callback() {
        override fun getOldListSize() = old.size
        override fun getNewListSize() = new.size

        override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
            return old[oldItemPosition] == new[newItemPosition]
        }

        override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
            return areItemsTheSame(oldItemPosition, newItemPosition)
        }
    }

    inner class ViewHolder(private val textView: TextView) : RecyclerView.ViewHolder(textView) {
        init {
            textView.setOnClickListener(containerOnItemClickListener)
        }

        fun setText(text: String) {
            textView.text = text
        }
    }

    private val containerOnItemClickListener = View.OnClickListener {
        val text = (it as TextView).text.toString()

        onItemClickListener(text)
    }

    private var elements = FilteredArray.empty<String>()

    fun submitData(elements: FilteredArray<String>) {
        val result = DiffUtil.calculateDiff(Callback(this.elements, elements))
        this.elements = elements

        result.dispatchUpdatesTo(this)
    }

    override fun getItemCount() = elements.size

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val context = parent.context
        val padding = context.resources.getDimensionPixelOffset(R.dimen.badgeSelectorDialog_listElementPadding)
        val textView = MaterialTextView(context).apply {
            layoutParams = ITEM_LAYOUT_PARAMS

            setPadding(padding)
            TextViewCompat.setTextAppearance(
                this,
                com.google.android.material.R.style.TextAppearance_Material3_BodyLarge
            )
        }

        return ViewHolder(textView)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val element = elements[position]

        holder.setText(element)
    }

    companion object {
        private val ITEM_LAYOUT_PARAMS = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )
    }
}