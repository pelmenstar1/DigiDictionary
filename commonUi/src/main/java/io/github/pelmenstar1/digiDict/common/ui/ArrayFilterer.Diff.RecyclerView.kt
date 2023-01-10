package io.github.pelmenstar1.digiDict.common.ui

import androidx.recyclerview.widget.RecyclerView
import io.github.pelmenstar1.digiDict.common.ListUpdateCallback

class RecyclerViewAdapterListUpdateCallback(private val adapter: RecyclerView.Adapter<*>): ListUpdateCallback {
    override fun onInserted(position: Int, count: Int) {
        adapter.notifyItemRangeInserted(position, count)
    }

    override fun onRemoved(position: Int, count: Int) {
        adapter.notifyItemRangeRemoved(position, count)
    }

    override fun onChanged(position: Int, count: Int) {
        adapter.notifyItemRangeChanged(position, count)
    }
}