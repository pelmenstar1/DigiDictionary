package io.github.pelmenstar1.digiDict.common

import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.shareIn

typealias DataLoadStateFlow<T> = Flow<DataLoadState<T>>

sealed interface DataLoadState<out T> {
    /**
     * [Loading] and [Error] carry no value, so they are declared as `DataLoadState<Nothing>`. As
     * the type parameter is covariant, that already makes them usable as a `DataLoadState<T>` for
     * any [T], which is what the shared instances behind the previous `loading()` and `error()`
     * factories achieved with an unchecked cast.
     */
    data object Loading : DataLoadState<Nothing>

    /**
     * @param cause what made the load fail. It is kept so that a caller can report or inspect it;
     * dropping it here used to leave nothing to attach to a crash report.
     */
    data class Error(val cause: Throwable) : DataLoadState<Nothing>

    data class Success<T>(val value: T) : DataLoadState<T>
}

private const val LOG_LOAD_STATES = false

class DataLoadStateManager<T>(val logTag: String) {
    class FlowBuilder<T>(val manager: DataLoadStateManager<T>) {
        fun fromAction(block: suspend () -> T): DataLoadStateFlow<T> {
            return flow {
                logLoading()
                emit(DataLoadState.Loading)

                try {
                    val value = block()

                    logSuccess(value)
                    emit(DataLoadState.Success(value))
                } catch (e: Exception) {
                    logError(e)

                    emit(DataLoadState.Error(e))
                }
            }
        }

        fun fromFlow(flow: Flow<T>): DataLoadStateFlow<T> {
            return flow.map<T, DataLoadState<T>> {
                logSuccess(it)

                DataLoadState.Success(it)
            }.onStart {
                logLoading()

                emit(DataLoadState.Loading)
            }.catch { e ->
                logError(e)

                emit(DataLoadState.Error(e))
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

                flowOf(DataLoadState.Error(e))
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
        }.shareIn(scope, SharingStarted.Eagerly, replay = 1)
    }

    fun retry() {
        retryFlow.value = Any()
    }
}

/**
 * Returns the value of the first success state emitted by the flow.
 */
suspend fun <T> Flow<DataLoadState<T>>.firstSuccess(): T {
    return (first { it is DataLoadState.Success<T> } as DataLoadState.Success<T>).value
}

/**
 * Returns the value of most recent state emitted by the flow if it's of type [DataLoadState.Success].
 * Otherwise, returns `null`.
 */
fun <T> SharedFlow<DataLoadState<T>>.tryGetSuccess(): T? {
    val state = replayCache.getOrNull(0)

    return (state as? DataLoadState.Success<T>?)?.value
}