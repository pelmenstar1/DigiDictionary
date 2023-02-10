package io.github.pelmenstar1.digiDict.ui.home

import android.util.SparseArray
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.LinearLayout
import androidx.paging.PagingDataAdapter
import androidx.recyclerview.widget.RecyclerView
import io.github.pelmenstar1.digiDict.common.getLazyValue
import io.github.pelmenstar1.digiDict.ui.paging.*
import io.github.pelmenstar1.digiDict.ui.record.*

class HomeAdapter(
    onViewRecord: (id: Int) -> Unit
) : PagingDataAdapter<PageItem, HomeAdapter.HomeItemViewHolder>(HomePageItemDiffCallback) {
    inner class HomeItemViewHolder(private val container: ViewGroup) : RecyclerView.ViewHolder(container) {
        private var type = TYPE_NONE
        private val views = SparseArray<View>(4)

        @Suppress("UNCHECKED_CAST")
        fun bind(item: PageItem?) {
            if (item != null) {
                val context = container.context
                val inflater = getInflater(item) as PageItemInflater<PageItem, Any>
                val id = inflater.uniqueId
                val staticInfo = itemStaticInfoArray.getOrAdd(id) { inflater.createStaticInfo(context) }
                val view = views.getOrAdd(id) { inflater.createView(context, staticInfo) }

                replaceViewIfTypeDiffers(id, view)

                inflater.bind(view, item, itemInflaterArgs, staticInfo)
            } else {
                type = TYPE_NONE
                container.removeAllViews()
            }
        }

        private fun getInflater(item: PageItem): PageItemInflater<*, *> = when (item) {
            is PageItem.Record -> RecordItemInflater
            is PageItem.DateMarker -> DateMarkerInflater
            is PageItem.EventMarker -> EventMarkerInflater
        }

        private fun replaceViewIfTypeDiffers(expectedType: Int, view: View) {
            if (type != expectedType) {
                container.removeAllViews()
                container.addView(view)

                type = expectedType
            }
        }

        private inline fun <T : Any> SparseArray<T>.getOrAdd(key: Int, create: () -> T): T {
            return getLazyValue(get(key), create) { set(key, it) }
        }
    }

    private val onItemClickListener = ConciseRecordWithBadgesViewHolder.createOnItemClickListener(onViewRecord)
    private val itemInflaterArgs = PageItemInflaterArgs(onItemClickListener)
    private val itemStaticInfoArray = SparseArray<Any>(4)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HomeItemViewHolder {
        val context = parent.context
        val container = FrameLayout(context).apply {
            layoutParams = CONTAINER_LAYOUT_PARAMS
        }

        return HomeItemViewHolder(container)
    }

    override fun onBindViewHolder(holder: HomeItemViewHolder, position: Int) {
        val item = getItem(position)

        holder.bind(item)
    }

    companion object {
        private val CONTAINER_LAYOUT_PARAMS = LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT,
        )

        private const val TYPE_NONE = 0
    }
}