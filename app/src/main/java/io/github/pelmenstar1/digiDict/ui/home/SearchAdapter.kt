package io.github.pelmenstar1.digiDict.ui.home

import android.view.ViewGroup
import androidx.annotation.MainThread
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import io.github.pelmenstar1.digiDict.data.Record
import io.github.pelmenstar1.digiDict.ui.record.FilteredArrayRecordDiffCallback
import io.github.pelmenstar1.digiDict.ui.record.RecordViewHolder
import io.github.pelmenstar1.digiDict.utils.FilteredArray
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

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

        val diffResult = DiffUtil.calculateDiff(FilteredArrayRecordDiffCallback(data, newData))

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