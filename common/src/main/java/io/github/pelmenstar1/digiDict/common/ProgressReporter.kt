package io.github.pelmenstar1.digiDict.common

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

class ProgressReporter {
    private val _progressFlow: MutableStateFlow<Float>

    val progressFlow: StateFlow<Float>
        get() = _progressFlow

    private val _completed: Float
    private val _target: Float
    private val _diff: Float

    constructor() {
        _progressFlow = MutableStateFlow(UNREPORTED)

        _completed = 0f
        _target = 1f
        _diff = 1f
    }

    private constructor(pFlow: MutableStateFlow<Float>, completed: Float, target: Float) {
        _progressFlow = pFlow

        _completed = completed
        _target = target
        _diff = target - completed
    }

    fun onProgress(value: Float) {
        if (value !in 0f..1f) {
            throw IllegalArgumentException("value is out of range")
        }

        _progressFlow.value = _completed + _diff * value
    }

    fun start() {
        _progressFlow.value = _completed
    }

    fun end() {
        _progressFlow.value = _target
    }

    fun reset() {
        _progressFlow.value = UNREPORTED
    }

    fun reportError() {
        _progressFlow.value = ERROR
    }

    fun subReporter(completed: Float, target: Float): ProgressReporter {
        when {
            completed !in 0f..1f -> throw IllegalArgumentException("completed is out of range")
            target <= 0f || target > 1f -> throw IllegalArgumentException("target is out of range")
            completed >= target -> throw IllegalArgumentException("completed >= target")
        }

        val currentDiff = _diff
        val completedDelta = currentDiff * completed
        val targetDelta = currentDiff * (1f - target)

        return ProgressReporter(_progressFlow, _completed + completedDelta, _target - targetDelta)
    }

    companion object {
        const val UNREPORTED = -1f
        const val ERROR = -2f
    }
}

inline fun <R> trackProgressWith(
    startProgress: Float,
    endProgress: Float,
    reporter: ProgressReporter?,
    block: () -> R
): R {
    contract {
        callsInPlace(block, InvocationKind.EXACTLY_ONCE)
    }

    try {
        reporter?.onProgress(startProgress)
        val result = block()
        reporter?.onProgress(endProgress)

        return result
    } catch (th: Throwable) {
        reporter?.reportError()
        throw th
    }
}

inline fun <R> trackProgressWith(reporter: ProgressReporter?, block: () -> R): R {
    contract {
        callsInPlace(block, InvocationKind.EXACTLY_ONCE)
    }

    return trackProgressWith(startProgress = 0f, endProgress = 1f, reporter, block)
}

inline fun trackLoopProgressWith(reporter: ProgressReporter?, size: Int, loopBody: (index: Int) -> Unit) {
    contract {
        callsInPlace(loopBody, InvocationKind.UNKNOWN)
    }

    val fSize = size.toFloat()

    trackProgressWith(reporter) {
        for (i in 0 until size) {
            loopBody(i)
            reporter?.onProgress(i / fSize)
        }
    }
}

inline fun <T> trackLoopProgressWith(
    reporter: ProgressReporter?,
    elements: Array<out T>,
    loopBody: (index: Int, element: T) -> Unit
) {
    contract {
        callsInPlace(loopBody, InvocationKind.UNKNOWN)
    }

    trackLoopProgressWith(reporter, elements.size) { i ->
        loopBody(i, elements[i])
    }
}

inline fun trackLoopProgressWithSubReporters(
    reporter: ProgressReporter?,
    size: Int,
    loopBody: (index: Int, subReporter: ProgressReporter?) -> Unit
) {
    contract {
        callsInPlace(loopBody, InvocationKind.UNKNOWN)
    }

    val fSize = size.toFloat()

    trackProgressWith(reporter) {
        for (i in 0 until size) {
            val subReporter = reporter?.subReporter(i / fSize, (i + 1) / fSize)

            loopBody(i, subReporter)
        }
    }
}