package io.github.pelmenstar1.digiDict.common

import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.textfield.TextInputLayout
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

fun <T> CoroutineScope.launchFlowCollector(flow: Flow<T>, collector: FlowCollector<T>): Job {
    return launch {
        flow.collect(collector)
    }
}

fun <T : Enum<T>> CoroutineScope.launchErrorFlowCollector(
    inputLayout: TextInputLayout,
    flow: Flow<T?>,
    errorMapper: MessageMapper<in T>
) {
    launchFlowCollector(flow) { errorType ->
        inputLayout.error = errorType?.let(errorMapper::map)
    }
}

fun CoroutineScope.launchSetEnabledFlowCollector(
    view: View,
    flow: Flow<Boolean>
) {
    launchFlowCollector(flow) { view.isEnabled = it }
}

fun <T> CoroutineScope.launchSetEnabledIfEquals(view: View, value: T, flow: Flow<T>) {
    launchFlowCollector(flow) {
        view.isEnabled = it == value
    }
}

/**
 * Launches flow collector which shows snackbar on each message.
 *
 * @param flow flow which consists of enum [T] describing a message
 * @param messageMapper used to convert value [T] to string
 * @param container container in which show snackbar
 */
fun <T : Enum<T>> LifecycleOwner.launchMessageFlowCollector(
    flow: Flow<T?>,
    messageMapper: MessageMapper<in T>,
    container: ViewGroup?
) {
    if (container != null) {
        lifecycleScope.launchFlowCollector(flow) { type ->
            if (type != null) {
                val message = messageMapper.map(type)

                Snackbar.make(container, message, Snackbar.LENGTH_LONG).also {
                    it.showLifecycleAwareSnackbar(lifecycle)
                }
            }
        }
    }
}

inline fun MutableStateFlow<Int?>.updateNullable(func: (Int) -> Int) {
    update {
        val resolved = it ?: 0

        func(resolved)
    }
}

/**
 * Returns a flow which contains first elements of receiver flow that **do not** satisfy [condition].
 * The important part is that it lets the element on which [condition] returns true to be emitted to the output flow.
 */
inline fun <T> Flow<T>.cancelAfter(crossinline condition: (T) -> Boolean) = transformWhile { value ->
    emit(value)
    !condition(value)
}
