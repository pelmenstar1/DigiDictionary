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

sealed class ViewModelAction(protected val vm: ViewModel, protected val logTag: String) {
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
            vm.viewModelScope.launch {
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
    logTag: String,
) : ViewModelAction(vm, logTag) {
    fun run() {
        runInternal { invokeAction() }
    }

    protected abstract suspend fun invokeAction()
}

abstract class SingleArgumentViewModelAction<T>(
    vm: ViewModel,
    logTag: String
) : ViewModelAction(vm, logTag) {
    fun run(arg: T) {
        runInternal { invokeAction(arg) }
    }

    protected abstract suspend fun invokeAction(arg: T)
}

@JvmName("noArgViewModelAction")
inline fun ViewModel.viewModelAction(
    logTag: String,
    crossinline action: suspend () -> Unit
): NoArgumentViewModelAction {
    val vm = this

    return object : NoArgumentViewModelAction(vm, logTag) {
        override suspend fun invokeAction() = action()
    }
}

@JvmName("singleArgViewModelAction")
inline fun <T> ViewModel.viewModelAction(
    logTag: String,
    crossinline action: suspend (T) -> Unit
): SingleArgumentViewModelAction<T> {
    val vm = this

    return object : SingleArgumentViewModelAction<T>(vm, logTag) {
        override suspend fun invokeAction(arg: T) = action(arg)
    }
}