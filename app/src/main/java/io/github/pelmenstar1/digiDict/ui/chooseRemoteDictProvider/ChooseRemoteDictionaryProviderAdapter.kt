package io.github.pelmenstar1.digiDict.ui.chooseRemoteDictProvider

import android.content.Context
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.annotation.IdRes
import androidx.annotation.StyleRes
import androidx.core.widget.TextViewCompat
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.textview.MaterialTextView
import io.github.pelmenstar1.digiDict.R
import io.github.pelmenstar1.digiDict.common.ui.MaterialTextAppearanceSelector
import io.github.pelmenstar1.digiDict.common.ui.setPaddingRes
import io.github.pelmenstar1.digiDict.data.RemoteDictionaryProviderInfo

class ChooseRemoteDictionaryProviderAdapter(
    private val onProviderChosen: (RemoteDictionaryProviderInfo) -> Unit
) : RecyclerView.Adapter<ChooseRemoteDictionaryProviderAdapter.ViewHolder>() {
    inner class ViewHolder(private val container: ViewGroup) : RecyclerView.ViewHolder(container) {
        private val nameView = container.findViewById<TextView>(R.id.itemRemoteDictProvider_name)
        private val schemaView = container.findViewById<TextView>(R.id.itemRemoteDictProvider_schema)

        init {
            container.setOnClickListener(itemOnClickListener)
        }

        fun bind(provider: RemoteDictionaryProviderInfo) {
            nameView.text = provider.name
            schemaView.text = provider.resolvedUrl(query)

            container.tag = provider
        }
    }

    private val itemOnClickListener = View.OnClickListener {
        onProviderChosen(it.tag as RemoteDictionaryProviderInfo)
    }

    private var items = emptyArray<RemoteDictionaryProviderInfo>()

    /**
     * Should be set before submitItems()
     */
    var query: String = ""

    fun submitItems(items: Array<RemoteDictionaryProviderInfo>) {
        this.items = items

        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val container = createItemContainer(parent.context)

        return ViewHolder(container)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount() = items.size

    companion object {
        private val itemLayoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )

        private val nameLayoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )

        internal fun createItemContainer(context: Context): ViewGroup {
            val res = context.resources

            return LinearLayout(context).apply {
                layoutParams = itemLayoutParams
                orientation = LinearLayout.VERTICAL
                setPaddingRes(R.dimen.itemRemoteDictProvider_padding)

                addView(MaterialTextView(context).apply {
                    layoutParams = nameLayoutParams

                    initTextView(R.id.itemRemoteDictProvider_name) { BodyLarge }
                })

                addView(MaterialTextView(context).apply {
                    layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    ).apply {
                        topMargin = res.getDimensionPixelOffset(R.dimen.itemRemoteDictProvider_schemaTopMargin)
                    }

                    initTextView(R.id.itemRemoteDictProvider_schema) { BodySmall }
                })
            }
        }

        private inline fun TextView.initTextView(@IdRes id: Int, block: MaterialTextAppearanceSelector.() -> Int) {
            initTextView(id, MaterialTextAppearanceSelector.block())
        }

        private fun TextView.initTextView(@IdRes id: Int, @StyleRes textAppearance: Int) {
            this.id = id
            setTextIsSelectable(false)
            TextViewCompat.setTextAppearance(this, textAppearance)
        }
    }
}