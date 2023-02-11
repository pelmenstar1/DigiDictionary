package io.github.pelmenstar1.digiDict.ui.manageEvents

import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import io.github.pelmenstar1.digiDict.data.EventInfo
import io.github.pelmenstar1.digiDict.ui.ArrayDiffCallback

class ManageEventsAdapter(
    private val executeMenuAction: (eventId: Int, actionId: Int) -> Unit,
    private val stopEvent: (eventId: Int) -> Unit,
    private val onEventClickListener: (eventId: Int) -> Unit
) : RecyclerView.Adapter<ManageEventsAdapter.ViewHolder>() {
    class ViewHolder(private val container: ManageEventItemContainer) : RecyclerView.ViewHolder(container) {
        fun bind(event: EventInfo, isBeforeEventNotEnded: Boolean) {
            container.eventId = event.id
            container.name = event.name
            container.isEventNotEnded = event.endEpochSeconds == -1L
            container.isBeforeEventNotEnded = isBeforeEventNotEnded
        }
    }

    private val onContainerClickListener = View.OnClickListener {
        onEventClickListener((it as ManageEventItemContainer).eventId)
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
            onExecuteAction = executeMenuAction

            setOnClickListener(onContainerClickListener)
        }

        return ViewHolder(container)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val elements = elements

        var isBeforeEventNotEnded = false
        if (position < elements.size - 1) {
            isBeforeEventNotEnded = elements[position + 1].endEpochSeconds < 0
        }

        holder.bind(elements[position], isBeforeEventNotEnded)
    }
}