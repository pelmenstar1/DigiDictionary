package io.github.pelmenstar1.digiDict.common.ui

import android.view.View
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import io.github.pelmenstar1.digiDict.common.ValidityFlow
import kotlinx.coroutines.launch

/**
 * Collects the changes of [flow] for as long as [owner] is alive and sets whether [View] is enabled based on validity.
 */
fun View.setEnabledWhenValid(flow: ValidityFlow, owner: LifecycleOwner) {
    owner.lifecycleScope.launch {
        flow.collect { bits ->
            val scheme = flow.scheme

            // Don't change whether the view is enabled if the result is not computed yet
            // to avoid view blinking. Anyway, in some time, validity must change to be computed
            if (ValidityFlow.isAllComputed(bits, scheme)) {
                isEnabled = ValidityFlow.isAllValid(bits, scheme)
            }
        }
    }
}

fun View.setEnabledWhenFieldValid(flow: ValidityFlow, field: ValidityFlow.Field, owner: LifecycleOwner) {
    owner.lifecycleScope.launch {
        flow.collect { bits ->
            // Don't change whether the view is enabled if the result is not computed yet
            // to avoid view blinking. Anyway, in some time, validity must change to be computed
            if (ValidityFlow.isComputed(bits, field)) {
                isEnabled = ValidityFlow.isValid(bits, field)
            }
        }
    }
}
