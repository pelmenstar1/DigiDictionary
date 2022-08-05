package io.github.pelmenstar1.digiDict.utils

import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.*

typealias DataLoadStateFlow<T> = Flow<DataLoadState<T>>

sealed class DataLoadState<out T> {
    class Loading<T> internal constructor() : DataLoadState<T>() {
        override fun toString() = "DataLoadState.Loading"
    }

    class Error<T> internal constructor() : DataLoadState<T>() {
        override fun toString() = "DataLoadState.Error"
    }

    data class Success<T>(val value: T) : DataLoadState<T>()

    @Suppress("UNCHECKED_CAST")
    companion object {
        private val LOADING = Loading<Any>()
        private val ERROR = Error<Any>()

        fun <T> loading() = LOADING as Loading<T>
        fun <T> error() = ERROR as Error<T>
    }
}

class DataLoadStateManager<T>(val logTag: String) {
    class FlowBuilder<T>(private val manager: DataLoadStateManager<T>) {
        fun fromAction(block: suspend () -> T): DataLoadStateFlow<T> {
            return flow {
                logLoading()
                emit(DataLoadState.loading())

                try {
                    val value = block()

                    logSuccess(value)
                    emit(DataLoadState.Success(value))
                } catch (e: Exception) {
                    logError(e)

                    emit(DataLoadState.error())
                }
            }
        }

        fun fromFlow(flow: Flow<T>): DataLoadStateFlow<T> {
            return flow.map<T, DataLoadState<T>> {
                logSuccess(it)

                DataLoadState.Success(it)
            }.onStart {
                logLoading()

                emit(DataLoadState.loading())
            }.catch { e ->
                logError(e)

                emit(DataLoadState.error())
            }
        }

        private inline fun logLoadState(getInfo: () -> String) {
            debugLog(manager.logTag) {
                info("loadState=${getInfo()}")
            }
        }

        private fun logLoading() = logLoadState { "Loading" }
        private fun logSuccess(value: T) = logLoadState { "Success(value=$value)" }

        private fun logError(e: Throwable) {
            Log.e(manager.logTag, "", e)
        }

        inline fun fromFlow(flowProvider: () -> Flow<T>): DataLoadStateFlow<T> {
            return fromFlow(flowProvider())
        }
    }

    private enum class RetryState { IDLE, RETRY }

    private val retryFlow = MutableStateFlow(RetryState.RETRY)

    fun buildFlow(
        scope: CoroutineScope,
        provider: FlowBuilder<T>.() -> DataLoadStateFlow<T>
    ): StateFlow<DataLoadState<T>> {
        val builder = FlowBuilder(this)

        return retryFlow.filter {
            it == RetryState.RETRY
        }.onEach {
            retryFlow.value = RetryState.IDLE
        }.flatMapMerge {
            builder.provider()
        }.stateIn(scope, SharingStarted.Lazily, DataLoadState.loading())
    }

    fun retry() {
        debugLog(logTag) {
            infoIf(
                retryFlow.value == RetryState.RETRY,
                "Action can't be retried because it's already in RETRY state"
            )
        }

        retryFlow.value = RetryState.RETRY
    }
}

suspend fun <T> Flow<DataLoadState<T>>.firstSuccess(): T {
    return filterIsInstance<DataLoadState.Success<T>>().first().value
}