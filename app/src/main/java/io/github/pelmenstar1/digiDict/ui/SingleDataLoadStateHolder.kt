package io.github.pelmenstar1.digiDict.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.pelmenstar1.digiDict.utils.DataLoadState
import io.github.pelmenstar1.digiDict.utils.DataLoadStateManager
import kotlinx.coroutines.flow.Flow

interface SingleDataLoadStateHolder<T> {
    val dataStateFlow: Flow<DataLoadState<T>>

    fun retryLoadData()
}

abstract class SingleDataLoadStateViewModel<T>(logTag: String) : ViewModel(), SingleDataLoadStateHolder<T> {
    private val dataStateManager = DataLoadStateManager<T>(logTag)

    override val dataStateFlow = dataStateManager.buildFlow(viewModelScope) { buildDataFlow() }

    abstract fun DataLoadStateManager.FlowBuilder<T>.buildDataFlow(): Flow<DataLoadState<T>>

    override fun retryLoadData() {
        dataStateManager.retry()
    }
}