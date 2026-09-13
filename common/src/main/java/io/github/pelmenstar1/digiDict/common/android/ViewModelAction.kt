package io.github.pelmenstar1.digiDict.common.android

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.pelmenstar1.digiDict.common.ValidityFlow
import io.github.pelmenstar1.digiDict.common.getLazyValue
import io.sentry.kotlin.SentryContext
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext

/**
 * An operation started by a view-model, exposing its outcome through [successFlow] and [errorFlow].
 *
 * The action takes a single argument of type [T]. An action that needs no argument is a
 * [NoArgumentViewModelAction], which fixes [T] to [Unit]; one that needs several takes a type that
 * holds them. Use `ViewModelAction<*>` to refer to an action whose argument type does not matter.
 */
abstract class ViewModelAction<in T>(
    protected val vm: ViewModel,
    protected val coroutineContext: CoroutineContext,
    protected val logTag: String
) {
    protected val resultFlow = MutableSharedFlow<Any?>(replay = 1)
    protected val isActionStarted = AtomicBoolean()

    protected val isWaitingForValidityResult = AtomicBoolean()

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

    fun run(arg: T) {
        runInternal { invokeAction(arg) }
    }

    protected abstract suspend fun invokeAction(arg: T)

    protected inline fun runInternal(crossinline action: suspend () -> Unit) {
        if (isActionStarted.compareAndSet(false, true)) {
            launchInViewModelScope {
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

    protected inline fun runWhenValidInternal(flow: ValidityFlow, crossinline action: () -> Unit) {
        if (flow.isAllValid) {
            action()
        } else if (!flow.isAllComputed) {
            if (isWaitingForValidityResult.compareAndSet(false, true)) {
                launchInViewModelScope {
                    val value = flow.waitForAllComputedAndReturnIsAllValid()
                    isWaitingForValidityResult.set(false)

                    if (value) {
                        action()
                    }
                }
            }
        }
    }

    protected fun sentryAwareContext(): CoroutineContext = coroutineContext + SentryContext()

    protected inline fun launchInViewModelScope(crossinline action: suspend () -> Unit) {
        vm.viewModelScope.launch(sentryAwareContext()) {
            action()
        }
    }

    companion object {
        protected val SUCCESS = Any()
    }
}

/**
 * A [ViewModelAction] that takes no argument, so that it can be started with a plain [run] and
 * gated on a [ValidityFlow] with [runWhenValid].
 */
abstract class NoArgumentViewModelAction(
    vm: ViewModel,
    coroutineContext: CoroutineContext,
    logTag: String,
) : ViewModelAction<Unit>(vm, coroutineContext, logTag) {
    fun run() {
        run(Unit)
    }

    fun runWhenValid(flow: ValidityFlow) {
        runWhenValidInternal(flow, ::run)
    }

    final override suspend fun invokeAction(arg: Unit) = invokeAction()

    protected abstract suspend fun invokeAction()
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
): ViewModelAction<T> {
    val vm = this

    return object : ViewModelAction<T>(vm, coroutineContext, logTag) {
        override suspend fun invokeAction(arg: T) = action(arg)
    }
}
