package io.github.pelmenstar1.digiDict.ui.manageRecordBadges

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import io.github.pelmenstar1.digiDict.R
import io.github.pelmenstar1.digiDict.common.EmptyArray
import io.github.pelmenstar1.digiDict.common.getLazyValue

class ManageRecordBadgesAdapter(
    private val onRemove: (String) -> Unit
) : RecyclerView.Adapter<ManageRecordBadgesAdapter.ViewHolder>() {
    private class Callback(
        private val old: Array<out String>,
        private val new: Array<out String>
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

    inner class ViewHolder(private val container: ViewGroup) : RecyclerView.ViewHolder(container) {
        private val nameView = container.findViewById<TextView>(R.id.itemManageRecordBadges_nameView)
        private val removeButton = container.findViewById<Button>(R.id.itemManageRecordBadges_removeButton)

        init {
            removeButton.setOnClickListener(removeButtonOnClickListener)
        }

        fun bind(name: String) {
            container.tag = name
            nameView.text = name
        }
    }

    private val removeButtonOnClickListener = View.OnClickListener {
        val name = ((it.parent as ViewGroup).tag as String)

        onRemove(name)
    }

    private var elements: Array<out String> = EmptyArray.STRING

    private var layoutInflaterHolder: LayoutInflater? = null

    fun submitData(elements: Array<out String>) {
        val result = DiffUtil.calculateDiff(Callback(this.elements, elements))
        this.elements = elements

        result.dispatchUpdatesTo(this)
    }

    override fun getItemCount() = elements.size

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val layoutInflater = getLazyValue(
            layoutInflaterHolder,
            { LayoutInflater.from(parent.context) }
        ) { layoutInflaterHolder = it }

        val container = layoutInflater.inflate(R.layout.item_manage_record_badges, parent, false)
        container.layoutParams = ITEM_LAYOUT_PARAMS

        return ViewHolder(container as ViewGroup)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val element = elements[position]

        holder.bind(element)
    }

    companion object {
        private val ITEM_LAYOUT_PARAMS = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )
    }
}