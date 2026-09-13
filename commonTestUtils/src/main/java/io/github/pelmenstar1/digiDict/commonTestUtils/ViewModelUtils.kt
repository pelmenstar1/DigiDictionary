package io.github.pelmenstar1.digiDict.commonTestUtils

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelStore
import io.github.pelmenstar1.digiDict.common.android.NoArgumentViewModelAction
import io.github.pelmenstar1.digiDict.common.android.ViewModelAction
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.cancel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch

suspend fun ViewModelAction<*>.waitForResult() {
    try {
        coroutineScope {
           val waitScope = this

            launch { errorFlow.collect { throw it } }
            launch { successFlow.collect { waitScope.cancel() } }
        }
    } catch (e: Throwable) {
        if (e !is CancellationException) {
            throw e
        }
    }
}

suspend fun NoArgumentViewModelAction.runAndWaitForResult() {
    run()
    waitForResult()
}

suspend fun <T> ViewModelAction<T>.runAndWaitForResult(arg: T) {
    run(arg)
    waitForResult()
}

/**
 * Clears the view-model, cancelling its [androidx.lifecycle.viewModelScope] and invoking
 * [ViewModel.onCleared], the same way the owner of a real view-model store would.
 *
 * [ViewModel.clear] itself is internal to the lifecycle library, so the view-model is put into a
 * throwaway [ViewModelStore] and that store is cleared instead. Reaching for the internal method
 * through reflection would tie the tests to a name the library is free to change: it already did,
 * when the Kotlin rewrite mangled it to `clear$lifecycle_viewmodel`.
 */
fun ViewModel.clear() {
    ViewModelStore().also { it.put("vm", this) }.clear()
}

inline fun <T : ViewModel> T.use(block: (vm: T) -> Unit) {
    try {
        block(this)
    } finally {
        clear()
    }
}