package io.github.pelmenstar1.digiDict.ui.addEditRecord.badge

import android.content.Context
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.view.setPadding
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.textview.MaterialTextView
import io.github.pelmenstar1.digiDict.R
import io.github.pelmenstar1.digiDict.common.getLazyValue
import io.github.pelmenstar1.digiDict.common.textAppearance.TextAppearance
import io.github.pelmenstar1.digiDict.common.ui.ColorCircleView
import io.github.pelmenstar1.digiDict.common.ui.getTypedViewAt
import io.github.pelmenstar1.digiDict.data.RecordBadgeInfo

class BadgeSelectorDialogAdapter(
    private val onItemClickListener: (RecordBadgeInfo) -> Unit
) : RecyclerView.Adapter<BadgeSelectorDialogAdapter.ViewHolder>() {
    internal class StaticInfo(context: Context) {
        val nameTextAppearance = TextAppearance(context) { BodyLarge }
        val containerPadding: Int
        val nameMarginStart: Int
        val colorCircleSize: Int

        init {
            val res = context.resources

            containerPadding = res.getDimensionPixelOffset(R.dimen.badgeSelectorDialog_listElement_padding)
            nameMarginStart = res.getDimensionPixelOffset(R.dimen.badgeSelectorDialog_listElement_nameStartMargin)
            colorCircleSize = res.getDimensionPixelSize(R.dimen.badgeSelectorDialog_listElement_colorCircleSize)
        }
    }

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

    inner class ViewHolder(private val container: ViewGroup) : RecyclerView.ViewHolder(container) {
        private val nameView = container.getTypedViewAt<TextView>(NAME_INDEX)
        private val colorCircleView = container.getTypedViewAt<ColorCircleView>(COLOR_CIRCLE_INDEX)

        init {
            container.setOnClickListener(containerOnItemClickListener)
        }

        fun bind(value: RecordBadgeInfo) {
            container.tag = value

            nameView.text = value.name
            colorCircleView.color = value.outlineColor
        }
    }

    private val containerOnItemClickListener = View.OnClickListener {
        val badge = it.tag as RecordBadgeInfo

        onItemClickListener(badge)
    }

    private var elements = emptyList<RecordBadgeInfo>()
    private var staticInfo: StaticInfo? = null

    fun submitData(elements: List<RecordBadgeInfo>) {
        val result = DiffUtil.calculateDiff(Callback(this.elements, elements))
        this.elements = elements

        result.dispatchUpdatesTo(this)
    }

    override fun getItemCount() = elements.size

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val context = parent.context
        val si = getLazyValue(
            staticInfo,
            { StaticInfo(context) },
            { staticInfo = it }
        )

        return ViewHolder(createItemContainer(context, si))
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(elements[position])
    }

    companion object {
        private val ITEM_LAYOUT_PARAMS = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )

        private const val COLOR_CIRCLE_INDEX = 0
        private const val NAME_INDEX = 1

        internal fun createItemContainer(context: Context, staticInfo: StaticInfo): ViewGroup {
            return LinearLayout(context).apply {
                layoutParams = ITEM_LAYOUT_PARAMS
                orientation = LinearLayout.HORIZONTAL

                setPadding(staticInfo.containerPadding)

                addView(ColorCircleView(context).apply {
                    val size = staticInfo.colorCircleSize
                    layoutParams = LinearLayout.LayoutParams(size, size).apply {
                        gravity = Gravity.CENTER_VERTICAL
                    }
                })

                addView(MaterialTextView(context).apply {
                    layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    ).apply {
                        gravity = Gravity.CENTER_VERTICAL

                        leftMargin = staticInfo.nameMarginStart
                    }

                    staticInfo.nameTextAppearance.apply(this)
                })
            }
        }
    }
}