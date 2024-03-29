package io.github.pelmenstar1.digiDict.common

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.transformWhile
import kotlinx.coroutines.launch

fun <T> CoroutineScope.launchFlowCollector(flow: Flow<T>, collector: FlowCollector<T>): Job {
    return launch { flow.collect(collector) }
}

/**
 * Returns a flow which contains first elements of receiver flow that **do not** satisfy [condition].
 * The important part is that it lets the element on which [condition] returns true to be emitted to the output flow.
 */
inline fun <T> Flow<T>.cancelAfter(crossinline condition: (T) -> Boolean) = transformWhile { value ->
    emit(value)
    !condition(value)
}

fun Flow<Boolean>.filterTrue() = filter { it }