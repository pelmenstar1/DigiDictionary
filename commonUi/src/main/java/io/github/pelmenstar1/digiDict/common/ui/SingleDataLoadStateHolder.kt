package io.github.pelmenstar1.digiDict.common.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.pelmenstar1.digiDict.common.DataLoadState
import io.github.pelmenstar1.digiDict.common.DataLoadStateManager
import kotlinx.coroutines.flow.Flow

interface SingleDataLoadStateHolder<T> {
    val dataStateFlow: Flow<DataLoadState<T>>

    /**
     * Gets whether it's possible that new [DataLoadState], apart from [DataLoadState.Success], can be emitted after [DataLoadState.Success].
     * App state remains consistent if [canRefreshAfterSuccess] doesn't reflect real situation, but
     * it can lead to big performance drawback if [canRefreshAfterSuccess] is true but should be false.
     *
     * If it's very unlikely that new [DataLoadState] can be emitted after [DataLoadState.Success], it should be true.
     */
    val canRefreshAfterSuccess: Boolean

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