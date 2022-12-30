package io.github.pelmenstar1.digiDict.commonTestUtils

import androidx.lifecycle.ViewModel
import io.github.pelmenstar1.digiDict.common.NoArgumentViewModelAction
import io.github.pelmenstar1.digiDict.common.SingleArgumentViewModelAction
import io.github.pelmenstar1.digiDict.common.ViewModelAction
import io.github.pelmenstar1.digiDict.common.launchFlowCollector
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.cancel
import kotlinx.coroutines.coroutineScope

suspend fun ViewModelAction.waitForResult() {
    try {
        coroutineScope {
            launchFlowCollector(errorFlow) { throw it }
            launchFlowCollector(successFlow) {
                cancel()
            }
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

suspend fun <T> SingleArgumentViewModelAction<T>.runAndWaitForResult(arg: T) {
    run(arg)
    waitForResult()
}

fun ViewModel.clearThroughReflection() {
    val method = ViewModel::class.java.getDeclaredMethod("clear")
    method.isAccessible = true

    method.invoke(this)
}

inline fun <T : ViewModel> T.use(block: (vm: T) -> Unit) {
    try {
        block(this)
    } finally {
        clearThroughReflection()
    }
}