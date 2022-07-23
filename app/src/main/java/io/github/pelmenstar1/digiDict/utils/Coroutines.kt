package io.github.pelmenstar1.digiDict.utils

import android.view.ViewGroup
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.textfield.TextInputLayout
import io.github.pelmenstar1.digiDict.MessageMapper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

inline fun <T> CoroutineScope.launchFlowCollector(
    flow: Flow<T>,
    crossinline block: suspend (value: T) -> Unit
) {
    launch {
        flow.collect {
            block(it)
        }
    }
}

fun <T : Enum<T>> CoroutineScope.launchErrorFlowCollector(
    inputLayout: TextInputLayout,
    flow: Flow<T?>,
    errorMapper: MessageMapper<T>
) {
    launchFlowCollector(flow) { errorType ->
        inputLayout.error = errorType?.let(errorMapper::map)
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
    messageMapper: MessageMapper<T>,
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