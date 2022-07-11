package io.github.pelmenstar1.digiDict.utils

import androidx.appcompat.app.AlertDialog
import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlin.coroutines.resume

/**
 * Builder-method which creates dialog with lambda [createDialog]
 * and waits until confirm lambda passed to an argument to [createDialog] is called.
 *
 * The value passed to the confirm lambda is returned.
 */
suspend inline fun <T> waitUntilDialogAction(crossinline createDialog: (confirm: (T) -> Unit) -> AlertDialog): T {
    var continuation: CancellableContinuation<T>? = null

    val dialog = withContext(Dispatchers.Main) {
        createDialog {
            continuation?.resume(it)
            continuation = null
        }.also {
            it.show()
        }
    }

    return suspendCancellableCoroutine { cont ->
        continuation = cont

        cont.invokeOnCancellation {
            dialog.cancel()
        }
    }
}