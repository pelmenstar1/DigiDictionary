package io.github.pelmenstar1.digiDict.ui.chooseRemoteDictProvider

import android.content.Context
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.view.setPadding
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.textview.MaterialTextView
import io.github.pelmenstar1.digiDict.R
import io.github.pelmenstar1.digiDict.common.getLazyValue
import io.github.pelmenstar1.digiDict.common.textAppearance.TextAppearance
import io.github.pelmenstar1.digiDict.common.ui.getTypedViewAt
import io.github.pelmenstar1.digiDict.data.RemoteDictionaryProviderInfo

class ChooseRemoteDictionaryProviderAdapter(
    private val onProviderChosen: (RemoteDictionaryProviderInfo) -> Unit
) : RecyclerView.Adapter<ChooseRemoteDictionaryProviderAdapter.ViewHolder>() {
    private class StaticInfo(context: Context) {
        val itemPadding: Int
        val schemaLayoutParams: LinearLayout.LayoutParams

        val nameTextAppearance = TextAppearance(context) { BodyLarge }
        val schemaTextAppearance = TextAppearance(context) { BodySmall }

        init {
            with(context.resources) {
                itemPadding = getDimensionPixelOffset(R.dimen.itemRemoteDictProvider_padding)

                schemaLayoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    topMargin = getDimensionPixelOffset(R.dimen.itemRemoteDictProvider_schemaTopMargin)
                }
            }
        }
    }

    class ViewHolder(
        private val container: ViewGroup,
        private val query: String,
        itemOnClickListener: View.OnClickListener
    ) : RecyclerView.ViewHolder(container) {
        private val nameView = container.getTypedViewAt<TextView>(NAME_VIEW_INDEX)
        private val schemaView = container.getTypedViewAt<TextView>(SCHEMA_VIEW_INDEX)

        init {
            container.setOnClickListener(itemOnClickListener)
        }

        fun bind(provider: RemoteDictionaryProviderInfo) {
            container.tag = provider

            nameView.text = provider.name
            schemaView.text = provider.resolvedUrl(query)
        }
    }

    private val itemOnClickListener = View.OnClickListener {
        onProviderChosen(it.tag as RemoteDictionaryProviderInfo)
    }

    private var items = emptyArray<RemoteDictionaryProviderInfo>()

    private var staticInfo: StaticInfo? = null

    /**
     * Should be set before submitItems()
     */
    var query: String = ""

    /**
     * Submits given items to the adapter. If [newItems] is not empty, the method can be called only once.
     */
    fun submitItems(newItems: Array<RemoteDictionaryProviderInfo>) {
        if (items.isNotEmpty()) {
            throw IllegalStateException("submitItems() has been already called")
        }

        items = newItems
        notifyItemRangeInserted(0, newItems.size)
    }

    override fun getItemCount() = items.size

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val si = getLazyValue(
            staticInfo,
            { StaticInfo(parent.context) },
            { staticInfo = it }
        )

        val container = createItemContainer(parent.context, si)

        return ViewHolder(container, query, itemOnClickListener)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(items[position])
    }

    companion object {
        private val itemLayoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )

        private val nameLayoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )

        private const val NAME_VIEW_INDEX = 0
        private const val SCHEMA_VIEW_INDEX = 1

        private fun createItemContainer(context: Context, staticInfo: StaticInfo): ViewGroup {
            return LinearLayout(context).apply {
                layoutParams = itemLayoutParams
                orientation = LinearLayout.VERTICAL

                setPadding(staticInfo.itemPadding)

                addView(MaterialTextView(context).apply {
                    layoutParams = nameLayoutParams

                    initTextView(staticInfo.nameTextAppearance)
                })

                addView(MaterialTextView(context).apply {
                    layoutParams = staticInfo.schemaLayoutParams

                    initTextView(staticInfo.schemaTextAppearance)
                })
            }
        }

        private fun TextView.initTextView(textAppearance: TextAppearance) {
            setTextIsSelectable(false)
            textAppearance.apply(this)
        }
    }
}