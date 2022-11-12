package io.github.pelmenstar1.digiDict.common

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicBoolean

class ExclusiveWaitHandleForFlowCondition<TValue>(
    private val scope: CoroutineScope,
    private val flow: Flow<TValue>,
    private val stopExclusiveCondition: suspend (TValue) -> Boolean,
    private val runActionCondition: (TValue) -> Boolean,
    private val action: () -> Unit
) {
    private val isWaiting = AtomicBoolean()

    fun runAction() {
        if (isWaiting.compareAndSet(false, true)) {
            scope.launch {
                val value = flow.first(stopExclusiveCondition)
                isWaiting.set(false)

                if (runActionCondition(value)) {
                    action()
                }
            }
        }
    }
}