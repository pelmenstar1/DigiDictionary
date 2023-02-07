package io.github.pelmenstar1.digiDict.ui.manageEvents

import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import io.github.pelmenstar1.digiDict.data.EventInfo
import io.github.pelmenstar1.digiDict.ui.ArrayDiffCallback

class ManageEventsAdapter(
    private val executeAction: (itemId: Int, actionId: Int) -> Unit,
    private val stopEvent: (itemId: Int) -> Unit
) : RecyclerView.Adapter<ManageEventsAdapter.ViewHolder>() {
    inner class ViewHolder(private val container: ManageEventItemContainer) : RecyclerView.ViewHolder(container) {
        fun bind(event: EventInfo) {
            container.eventId = event.id
            container.name = event.name
            container.isEventNotEnded = event.endEpochSeconds == -1L
        }
    }

    private var elements: Array<out EventInfo> = emptyArray()

    fun submitElements(newElements: Array<out EventInfo>) {
        val diffResult = DiffUtil.calculateDiff(ArrayDiffCallback(elements, newElements))
        elements = newElements

        diffResult.dispatchUpdatesTo(this)
    }

    override fun getItemCount() = elements.size

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val container = ManageEventItemContainer(parent.context).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )

            setStopEventListener { stopEvent(eventId) }
            onExecuteAction = executeAction
        }

        return ViewHolder(container)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(elements[position])
    }
}