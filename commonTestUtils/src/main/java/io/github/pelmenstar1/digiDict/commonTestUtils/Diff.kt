package io.github.pelmenstar1.digiDict.commonTestUtils

import io.github.pelmenstar1.digiDict.common.ListUpdateCallback

object Diff {
    enum class RangeType {
        INSERTED,
        REMOVED,
        CHANGED
    }

    data class TypedIntRange(val type: RangeType, val position: Int, val count: Int) {
        override fun toString(): String {
            return "($type: pos=$position; count=$count)"
        }
    }

    class ListUpdateCallbackToList(private val list: MutableList<TypedIntRange>) : ListUpdateCallback {
        override fun onInserted(position: Int, count: Int) = onEvent(RangeType.INSERTED, position, count)
        override fun onRemoved(position: Int, count: Int) = onEvent(RangeType.REMOVED, position, count)
        override fun onChanged(position: Int, count: Int) = onEvent(RangeType.CHANGED, position, count)

        private fun onEvent(type: RangeType, position: Int, count: Int) {
            list.add(TypedIntRange(type, position, count))
        }
    }

    class RecyclerViewListUpdateCallbackToList(
        private val list: MutableList<TypedIntRange>
    ) : androidx.recyclerview.widget.ListUpdateCallback {
        override fun onInserted(position: Int, count: Int) = onEvent(RangeType.INSERTED, position, count)
        override fun onRemoved(position: Int, count: Int) = onEvent(RangeType.REMOVED, position, count)
        override fun onChanged(position: Int, count: Int, payload: Any?) = onEvent(RangeType.CHANGED, position, count)

        override fun onMoved(fromPosition: Int, toPosition: Int) {
            throw IllegalStateException("Move detection must be disabled")
        }

        private fun onEvent(type: RangeType, position: Int, count: Int) {
            list.add(TypedIntRange(type, position, count))
        }
    }
}