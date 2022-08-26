package io.github.pelmenstar1.digiDict.common

import android.view.View
import android.view.ViewGroup
import androidx.annotation.StringRes
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import com.google.android.material.snackbar.Snackbar

/**
 * Shows snackbar which will be dismissed when [Lifecycle.Event.ON_DESTROY] event happens in specified [lifecycle].
 * Useful in fragments.
 */
fun Snackbar.showLifecycleAwareSnackbar(lifecycle: Lifecycle) {
    show()

    val snackbar = this
    lifecycle.addObserver(object : LifecycleEventObserver {
        override fun onStateChanged(source: LifecycleOwner, event: Lifecycle.Event) {
            if (event == Lifecycle.Event.ON_DESTROY) {
                snackbar.dismiss()
                lifecycle.removeObserver(this)
            }
        }
    })
}

fun LifecycleOwner.showSnackbarEventHandlerOnError(
    vmAction: ViewModelAction,
    container: ViewGroup?,
    @StringRes msgId: Int,
    duration: Int = Snackbar.LENGTH_LONG,
    anchorView: View? = null,
    actionText: Int = -1,
    snackbarAction: View.OnClickListener? = null
) {
    if (container != null) {
        lifecycleScope.launchFlowCollector(vmAction.errorFlow) {
            Snackbar.make(container, msgId, duration).apply {
                anchorView?.let { setAnchorView(it) }
                snackbarAction?.let { setAction(actionText, snackbarAction) }
            }.showLifecycleAwareSnackbar(lifecycle)
        }
    }
}