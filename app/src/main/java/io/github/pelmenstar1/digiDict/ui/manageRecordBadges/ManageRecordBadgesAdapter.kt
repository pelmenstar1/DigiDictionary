package io.github.pelmenstar1.digiDict.ui.manageRecordBadges

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.annotation.IdRes
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import io.github.pelmenstar1.digiDict.R
import io.github.pelmenstar1.digiDict.common.getLazyValue
import io.github.pelmenstar1.digiDict.common.ui.ColorCircleView
import io.github.pelmenstar1.digiDict.data.RecordBadgeInfo
import io.github.pelmenstar1.digiDict.ui.ArrayDiffCallback

class ManageRecordBadgesAdapter(
    private val onAction: (actionId: Int, badge: RecordBadgeInfo) -> Unit
) : RecyclerView.Adapter<ManageRecordBadgesAdapter.ViewHolder>() {
    inner class ViewHolder(private val container: ViewGroup) : RecyclerView.ViewHolder(container) {
        private val nameView: TextView
        private val colorCircleView: ColorCircleView

        init {
            with(container) {
                nameView = findViewById(R.id.itemManageRecordBadges_nameView)
                colorCircleView = findViewById(R.id.itemManageRecordBadges_colorCircleView)
            }

            initActionButton(R.id.itemManageRecordBadges_removeButton)
            initActionButton(R.id.itemManageRecordBadges_editButton)
        }

        fun bind(value: RecordBadgeInfo) {
            container.tag = value
            nameView.text = value.name
            colorCircleView.color = value.outlineColor
        }

        private fun initActionButton(@IdRes id: Int) {
            container.findViewById<View>(id).also {
                it.setOnClickListener(actionButtonOnClickListener)
            }
        }
    }

    private val actionButtonOnClickListener = View.OnClickListener {
        val parent = it.parent as ViewGroup
        val badge = parent.tag as RecordBadgeInfo

        val actionId = when (it.id) {
            R.id.itemManageRecordBadges_removeButton -> ACTION_REMOVE
            R.id.itemManageRecordBadges_editButton -> ACTION_EDIT
            else -> throw IllegalStateException("Illegal id of action button")
        }

        onAction(actionId, badge)
    }

    private var elements: Array<out RecordBadgeInfo> = emptyArray()
    private var layoutInflaterHolder: LayoutInflater? = null

    fun submitData(elements: Array<out RecordBadgeInfo>) {
        val result = DiffUtil.calculateDiff(ArrayDiffCallback(this.elements, elements))
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
        holder.bind(elements[position])
    }

    companion object {
        private val ITEM_LAYOUT_PARAMS = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )

        const val ACTION_REMOVE = 0
        const val ACTION_EDIT = 1
    }
}