package io.github.pelmenstar1.digiDict.ui.home.search

import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import io.github.pelmenstar1.digiDict.common.FilteredArray
import io.github.pelmenstar1.digiDict.common.ui.AsyncDataDiffer
import io.github.pelmenstar1.digiDict.data.Record
import io.github.pelmenstar1.digiDict.ui.record.FilteredArrayRecordDiffCallback
import io.github.pelmenstar1.digiDict.ui.record.RecordViewHolder
import kotlinx.coroutines.CoroutineScope

class SearchAdapter(
    differScope: CoroutineScope,
    onViewRecord: (id: Int) -> Unit
) : RecyclerView.Adapter<RecordViewHolder>() {
    private val onItemClickListener = RecordViewHolder.createOnItemClickListener(onViewRecord)

    private val asyncDiffer = AsyncDataDiffer(this, differScope, FilteredArray.empty<Record>()) { oldData, newData ->
        FilteredArrayRecordDiffCallback(oldData, newData)
    }

    fun submitData(newData: FilteredArray<Record>) {
        asyncDiffer.submit(newData)
    }

    fun submitEmpty() {
        asyncDiffer.submitEmpty()
    }

    override fun getItemCount() = asyncDiffer.currentData.size

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecordViewHolder {
        return RecordViewHolder(parent.context)
    }

    override fun onBindViewHolder(holder: RecordViewHolder, position: Int) {
        val record = asyncDiffer.currentData[position]

        holder.bind(record, onItemClickListener)
    }
}