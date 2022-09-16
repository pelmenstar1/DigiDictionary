package io.github.pelmenstar1.digiDict.ui.importConfig

import android.annotation.SuppressLint
import android.content.Context
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.textview.MaterialTextView
import io.github.pelmenstar1.digiDict.R
import io.github.pelmenstar1.digiDict.backup.BackupFormat
import io.github.pelmenstar1.digiDict.common.ui.setPaddingRes
import io.github.pelmenstar1.digiDict.common.ui.setTextAppearance

class ImportFormatSelectorDialogAdapter(
    val onItemClickListener: (BackupFormat) -> Unit
) : RecyclerView.Adapter<ImportFormatSelectorDialogAdapter.ViewHolder>() {
    inner class ViewHolder(private val textView: TextView) : RecyclerView.ViewHolder(textView) {
        init {
            textView.setOnClickListener(itemOnClickListener)
        }

        fun bind(item: BackupFormat) {
            textView.apply {
                tag = item
                text = item.shortName
            }
        }
    }

    private val itemOnClickListener = View.OnClickListener {
        onItemClickListener(it.tag as BackupFormat)
    }

    private var items: Array<out BackupFormat> = emptyArray()

    @SuppressLint("NotifyDataSetChanged")
    fun submitItems(items: Array<out BackupFormat>) {
        this.items = items

        // submitItems() is expected to be called only once.
        notifyDataSetChanged()
    }

    override fun getItemCount() = items.size

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val textView = createTextView(parent.context)

        return ViewHolder(textView)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(items[position])
    }

    companion object {
        private val ITEM_LAYOUT_PARAMS = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )

        internal fun createTextView(context: Context): TextView {
            return MaterialTextView(context).apply {
                layoutParams = ITEM_LAYOUT_PARAMS

                textAlignment = TextView.TEXT_ALIGNMENT_CENTER
                setPaddingRes(R.dimen.importFormatSelectorDialog_entryPadding)
                setTextAppearance { BodyMedium }
            }
        }
    }
}