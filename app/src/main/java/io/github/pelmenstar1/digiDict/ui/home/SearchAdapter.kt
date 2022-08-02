package io.github.pelmenstar1.digiDict.ui.home

import android.view.ViewGroup
import androidx.annotation.MainThread
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import io.github.pelmenstar1.digiDict.data.Record
import io.github.pelmenstar1.digiDict.ui.record.RecordViewHolder
import io.github.pelmenstar1.digiDict.utils.FilteredArray
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class RecordDiffCallback(
    private val oldArray: FilteredArray<Record>,
    private val newArray: FilteredArray<Record>
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

class SearchAdapter(
    onViewRecord: (id: Int) -> Unit
) : RecyclerView.Adapter<RecordViewHolder>() {
    private val onItemClickListener = RecordViewHolder.createOnItemClickListener(onViewRecord)

    private var data = FilteredArray.empty<Record>()

    suspend fun submitData(newData: FilteredArray<Record>) {
        // Filtered arrays can be compared fast, if underlying arrays are equal by references which is true in most cases.
        if (data == newData) {
            return
        }

        val diffResult = DiffUtil.calculateDiff(RecordDiffCallback(data, newData))

        data = newData

        val adapter = this
        withContext(Dispatchers.Main) {
            diffResult.dispatchUpdatesTo(adapter)
        }
    }

    @MainThread
    fun submitEmpty() {
        val oldData = data
        data = FilteredArray.empty()

        notifyItemRangeRemoved(0, oldData.size)
    }

    override fun getItemCount() = data.size

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecordViewHolder {
        return RecordViewHolder(parent.context)
    }

    override fun onBindViewHolder(holder: RecordViewHolder, position: Int) {
        val record = data[position]

        holder.bind(record, onItemClickListener)
    }
}