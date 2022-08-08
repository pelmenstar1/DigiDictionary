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

private const val LOG_LOAD_STATES = false

class DataLoadStateManager<T>(val logTag: String) {
    class FlowBuilder<T>(val manager: DataLoadStateManager<T>) {
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
                infoIf(LOG_LOAD_STATES, "loadState=${getInfo()}")
            }
        }

        private fun logLoading() = logLoadState { "Loading" }
        private fun logSuccess(value: T) = logLoadState { "Success(value=$value)" }

        private fun logError(e: Throwable) {
            Log.e(manager.logTag, "", e)
        }

        inline fun fromFlow(flowProvider: () -> Flow<T>): DataLoadStateFlow<T> {
            return try {
                fromFlow(flowProvider())
            } catch (e: Exception) {
                Log.e(manager.logTag, "", e)

                flowOf(DataLoadState.error())
            }
        }
    }

    private val retryFlow = MutableStateFlow(Any())

    fun buildFlow(
        scope: CoroutineScope,
        provider: FlowBuilder<T>.() -> DataLoadStateFlow<T>
    ): SharedFlow<DataLoadState<T>> {
        val builder = FlowBuilder(this)

        return retryFlow.flatMapLatest {
            builder.provider()
        }.shareIn(scope, SharingStarted.Lazily, replay = 1)
    }

    fun retry() {
        retryFlow.value = Any()
    }
}

suspend fun <T> Flow<DataLoadState<T>>.firstSuccess(): T {
    return filterIsInstance<DataLoadState.Success<T>>().first().value
}