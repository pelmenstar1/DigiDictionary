package io.github.pelmenstar1.digiDict.common.ui

import androidx.annotation.MainThread
import androidx.recyclerview.widget.AdapterListUpdateCallback
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import io.github.pelmenstar1.digiDict.common.SizedIterable
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel

class AsyncDataDiffer<TData : SizedIterable<*>>(
    adapter: RecyclerView.Adapter<*>,
    private val scope: CoroutineScope,
    private val emptyData: TData,
    private val createCallback: (old: TData, new: TData) -> DiffUtil.Callback
) {
    private var isUpdateJobStarted = false
    private val updateChannel = Channel<TData>(Channel.CONFLATED)

    private val updateCallback = AdapterListUpdateCallback(adapter)

    @Volatile
    private var _currentData = emptyData

    val currentData: TData
        get() = _currentData

    @MainThread
    fun submit(newData: TData) {
        // As updateChannel's capacity is UNLIMITED, send is non-blocking.
        updateChannel.trySend(newData)

        startUpdateJobIfNecessary()
    }

    @MainThread
    fun submitEmpty() {
        // If 'update job' is not started, it means there were no updates and the data is already empty.
        if (isUpdateJobStarted) {
            updateChannel.trySend(emptyData)
        }
    }

    private fun startUpdateJobIfNecessary() {
        if (!isUpdateJobStarted) {
            isUpdateJobStarted = true

            scope.launch {
                while (isActive) {
                    val newData = updateChannel.receive()
                    val currentData = _currentData

                    val diffResult = DiffUtil.calculateDiff(createCallback(currentData, newData))

                    // If the channel isn't empty, it means that the channel contains fresher value and there's no
                    // sense in dispatching updates to adapter.
                    if (updateChannel.isEmpty) {
                        withContext(Dispatchers.Main) {
                            _currentData = newData

                            diffResult.dispatchUpdatesTo(updateCallback)
                        }
                    }
                }
            }
        }
    }
}