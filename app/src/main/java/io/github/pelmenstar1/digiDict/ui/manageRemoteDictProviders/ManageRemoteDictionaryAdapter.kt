package io.github.pelmenstar1.digiDict.ui.manageRemoteDictProviders

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import io.github.pelmenstar1.digiDict.R
import io.github.pelmenstar1.digiDict.data.RemoteDictionaryProviderInfo
import io.github.pelmenstar1.digiDict.utils.getLazyValue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private class RemoteDictProviderInfoCallback(
    private val oldArray: Array<RemoteDictionaryProviderInfo>,
    private val newArray: Array<RemoteDictionaryProviderInfo>
) : DiffUtil.Callback() {
    override fun getOldListSize() = oldArray.size
    override fun getNewListSize() = newArray.size

    override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
        return oldArray[oldItemPosition].id == newArray[newItemPosition].id
    }

    override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
        return oldArray[oldItemPosition] == newArray[newItemPosition]
    }
}

class ManageRemoteDictionaryAdapter(
    private val onDeleteProvider: (RemoteDictionaryProviderInfo) -> Unit
) : RecyclerView.Adapter<ManageRemoteDictionaryAdapter.ViewHolder>() {
    inner class ViewHolder(container: ViewGroup) : RecyclerView.ViewHolder(container) {
        private val nameView = container.findViewById<TextView>(R.id.itemManageRemoteDictProvider_name)
        private val schemaView = container.findViewById<TextView>(R.id.itemManageRemoteDictProvider_schema)
        private val deleteButton = container.findViewById<Button>(R.id.itemManageRemoteDictProvider_delete)

        fun bind(info: RemoteDictionaryProviderInfo) {
            nameView.text = info.name
            schemaView.text = info.schema

            deleteButton.apply {
                setOnClickListener(deleteButtonOnClickListener)
                tag = info
            }
        }
    }

    private val deleteButtonOnClickListener = View.OnClickListener {
        onDeleteProvider(it.tag as RemoteDictionaryProviderInfo)
    }
    private var items = emptyArray<RemoteDictionaryProviderInfo>()

    private var layoutInflater: LayoutInflater? = null

    suspend fun submitItems(items: Array<RemoteDictionaryProviderInfo>) {
        val oldItems = this.items
        val result = DiffUtil.calculateDiff(RemoteDictProviderInfoCallback(oldItems, items))

        this.items = items

        val adapter = this
        withContext(Dispatchers.Main) {
            result.dispatchUpdatesTo(adapter)
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val inflater = getLazyValue(
            layoutInflater,
            { LayoutInflater.from(parent.context) },
            { layoutInflater = it }
        )

        val container = inflater.inflate(R.layout.item_manage_remote_dict_provider, parent, false).also {
            it.layoutParams = itemLayoutParams
        }

        return ViewHolder(container as ViewGroup)
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
    }
}