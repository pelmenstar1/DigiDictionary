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

    // Stores whether the last event is ended.
    // Almost always reflects the state of the last elements in the elements.
    // But there can be cases like when the event is ended by user
    // when we opportunistically suppose that the event will be ended successfully and set it to true preliminary.
    //
    // That's done to correctly handle the situation when the event is ended, but isBeforeEventNotEnded property
    // in previous event is not updated due to the fact that event is not actually changed.
    private var isLastEventEnded = false

    private var elements: Array<out EventInfo> = emptyArray()

    /**
     * The method is called when the [stopEvent] lambda fails.
     */
    fun onStopEventOperationFailed() {
        // In onEventEnded, we set last bit but it's wrong by now as the operation failed. Fix it.
        setLastEventEndedStateAndUpdatePrevElement(lastEventState = false)
    }

    // Only last event can be ended
    private fun onLastEventEnded() {
        // Suppose that the event will be successfully ended now.
        setLastEventEndedStateAndUpdatePrevElement(lastEventState = true)
    }

    private fun setLastEventEndedStateAndUpdatePrevElement(lastEventState: Boolean) {
        val eventPos = elements.size - 1
        val posToUpdate = eventPos - 1

        isLastEventEnded = lastEventState

        if (posToUpdate >= 0) {
            notifyItemChanged(posToUpdate)
        }
    }

    fun submitElements(newElements: Array<out EventInfo>) {
        if (newElements.isNotEmpty()) {
            isLastEventEnded = newElements[newElements.size - 1].endEpochSeconds >= 0
        }

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

            setStopEventListener {
                onLastEventEnded()

                stopEvent(eventId)
            }
            onExecuteAction = executeMenuAction

            setOnClickListener(onContainerClickListener)
        }

        return ViewHolder(container)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val elements = elements

        var isBeforeEventNotEnded = false
        if (position == elements.size - 2) {
            isBeforeEventNotEnded = !isLastEventEnded
        }

        holder.bind(elements[position], isBeforeEventNotEnded)
    }
}