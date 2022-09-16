package io.github.pelmenstar1.digiDict.common

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext

sealed class ViewModelAction(
    protected val vm: ViewModel,
    protected val coroutineContext: CoroutineContext,
    protected val logTag: String
) {
    protected val resultFlow = MutableSharedFlow<Any?>(replay = 1)
    protected val isActionStarted = AtomicBoolean()

    private var _successFlow: Flow<Any>? = null
    private var _errorFlow: Flow<Throwable>? = null

    @Suppress("UNCHECKED_CAST")
    val successFlow: Flow<Any>
        get() = getLazyValue(
            _successFlow,
            { resultFlow.filter { it == SUCCESS } as Flow<Any> },
            { _successFlow = it }
        )

    val errorFlow: Flow<Throwable>
        get() = getLazyValue(
            _errorFlow,
            { resultFlow.filterIsInstance() },
            { _errorFlow = it }
        )

    protected inline fun runInternal(crossinline action: suspend () -> Unit) {
        if (isActionStarted.compareAndSet(false, true)) {
            vm.viewModelScope.launch(coroutineContext) {
                resultFlow.emit(null)
                try {
                    action()
                    resultFlow.emit(SUCCESS)
                } catch (e: Exception) {
                    if (e !is CancellationException) {
                        Log.e(logTag, "", e)

                        resultFlow.emit(e)
                    }
                } finally {
                    isActionStarted.set(false)
                }
            }
        }
    }

    companion object {
        protected val SUCCESS = Any()
    }
}

abstract class NoArgumentViewModelAction(
    vm: ViewModel,
    coroutineContext: CoroutineContext,
    logTag: String,
) : ViewModelAction(vm, coroutineContext, logTag) {
    fun run() {
        runInternal { invokeAction() }
    }

    protected abstract suspend fun invokeAction()
}

abstract class SingleArgumentViewModelAction<T>(
    vm: ViewModel,
    coroutineContext: CoroutineContext,
    logTag: String
) : ViewModelAction(vm, coroutineContext, logTag) {
    fun run(arg: T) {
        runInternal { invokeAction(arg) }
    }

    protected abstract suspend fun invokeAction(arg: T)
}

abstract class TwoArgumentViewModelAction<T1, T2>(
    vm: ViewModel,
    coroutineContext: CoroutineContext,
    logTag: String
) : ViewModelAction(vm, coroutineContext, logTag) {
    fun run(arg1: T1, arg2: T2) {
        runInternal { invokeAction(arg1, arg2) }
    }

    protected abstract suspend fun invokeAction(arg1: T1, arg2: T2)
}

abstract class ThreeArgumentViewModelAction<T1, T2, T3>(
    vm: ViewModel,
    coroutineContext: CoroutineContext,
    logTag: String
) : ViewModelAction(vm, coroutineContext, logTag) {
    fun run(arg1: T1, arg2: T2, arg3: T3) {
        runInternal { invokeAction(arg1, arg2, arg3) }
    }

    protected abstract suspend fun invokeAction(arg1: T1, arg2: T2, arg3: T3)
}

@JvmName("noArgViewModelAction")
inline fun ViewModel.viewModelAction(
    logTag: String,
    coroutineContext: CoroutineContext = EmptyCoroutineContext,
    crossinline action: suspend () -> Unit
): NoArgumentViewModelAction {
    val vm = this

    return object : NoArgumentViewModelAction(vm, coroutineContext, logTag) {
        override suspend fun invokeAction() = action()
    }
}

@JvmName("singleArgViewModelAction")
inline fun <T> ViewModel.viewModelAction(
    logTag: String,
    coroutineContext: CoroutineContext = EmptyCoroutineContext,
    crossinline action: suspend (T) -> Unit
): SingleArgumentViewModelAction<T> {
    val vm = this

    return object : SingleArgumentViewModelAction<T>(vm, coroutineContext, logTag) {
        override suspend fun invokeAction(arg: T) = action(arg)
    }
}

@JvmName("twoArgViewModelAction")
inline fun <T1, T2> ViewModel.viewModelAction(
    logTag: String,
    coroutineContext: CoroutineContext = EmptyCoroutineContext,
    crossinline action: suspend (T1, T2) -> Unit
): TwoArgumentViewModelAction<T1, T2> {
    val vm = this

    return object : TwoArgumentViewModelAction<T1, T2>(vm, coroutineContext, logTag) {
        override suspend fun invokeAction(arg1: T1, arg2: T2) = action(arg1, arg2)
    }
}

@JvmName("threeArgViewModelAction")
inline fun <T1, T2, T3> ViewModel.viewModelAction(
    logTag: String,
    coroutineContext: CoroutineContext = EmptyCoroutineContext,
    crossinline action: suspend (T1, T2, T3) -> Unit
): ThreeArgumentViewModelAction<T1, T2, T3> {
    val vm = this

    return object : ThreeArgumentViewModelAction<T1, T2, T3>(vm, coroutineContext, logTag) {
        override suspend fun invokeAction(arg1: T1, arg2: T2, arg3: T3) = action(arg1, arg2, arg3)
    }
}