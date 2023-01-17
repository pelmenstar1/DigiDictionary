package io.github.pelmenstar1.digiDict.common

interface ListUpdateCallback {
    fun onInserted(position: Int, count: Int)
    fun onRemoved(position: Int, count: Int)
    fun onChanged(position: Int, count: Int)
}