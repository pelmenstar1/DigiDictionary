package io.github.pelmenstar1.digiDict.utils

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.LifecycleOwner
import com.google.android.material.snackbar.Snackbar

/**
 * Shows snackbar which will be dismissed when [Lifecycle.Event.ON_DESTROY] event happens in specified [lifecycle].
 * Useful in fragments.
 */
fun Snackbar.showLifecycleAwareSnackbar(lifecycle: Lifecycle) {
    show()

    val snackbar = this
    lifecycle.addObserver(object: LifecycleEventObserver {
        override fun onStateChanged(source: LifecycleOwner, event: Lifecycle.Event) {
            if(event == Lifecycle.Event.ON_DESTROY) {
                snackbar.dismiss()
                lifecycle.removeObserver(this)
            }
        }
    })
}