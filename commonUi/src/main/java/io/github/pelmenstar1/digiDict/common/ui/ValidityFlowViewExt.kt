package io.github.pelmenstar1.digiDict.common.ui

import android.view.View
import io.github.pelmenstar1.digiDict.common.ValidityFlow
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/**
 * Launches a coroutine on [scope] that collects the changes of [flow] and sets whether [View] is enabled based on validity.
 */
fun View.setEnabledWhenValid(flow: ValidityFlow, scope: CoroutineScope) {
    scope.launch {
        flow.collect { bits ->
            val scheme = flow.scheme

            isEnabled = if (ValidityFlow.isAllComputed(bits, scheme)) {
                ValidityFlow.isValid(bits, scheme)
            } else {
                // To avoid add button blinking when the checking takes too long
                // we make the button visually enabled if all fields except not computed ones are valid.
                ValidityFlow.isValidExceptNotComputedFields(bits, scheme)
            }
        }
    }
}