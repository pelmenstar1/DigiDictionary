package io.github.pelmenstar1.digiDict.common

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

class ProgressReporter {
    private val _progressFlow: MutableStateFlow<Int>

    val progressFlow: StateFlow<Int>
        get() = _progressFlow

    private val _completed: Int
    private val _target: Int
    private val _diff: Int

    constructor() {
        _progressFlow = MutableStateFlow(UNREPORTED)

        _completed = 0
        _target = 100
        _diff = 100
    }

    private constructor(pFlow: MutableStateFlow<Int>, completed: Int, target: Int) {
        _progressFlow = pFlow

        _completed = completed
        _target = target
        _diff = target - completed
    }

    fun onProgress(value: Int) {
        if (value !in 0..100) {
            throw IllegalArgumentException("Value is out of bounds")
        }

        _progressFlow.value = _completed + (_diff * value) / 100
    }

    fun onProgress(current: Int, total: Int) {
        val newProgress = _completed + (_diff * current) / total
        if (newProgress !in 0..100) {
            throw IllegalArgumentException("Illegal current and total: current=$current, total=$total")
        }

        _progressFlow.value = newProgress
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

    fun subReporter(completed: Int, target: Int): ProgressReporter {
        when {
            completed !in 0..100 -> throw IllegalArgumentException("completed is out of range")
            target < 0 || target > 100 -> throw IllegalArgumentException("target is out of range")
            completed > target -> throw IllegalArgumentException("completed > target")
        }

        val currentDiff = _diff
        val completedDelta = (currentDiff * completed) / 100
        val targetDelta = (currentDiff * (100 - target)) / 100

        return ProgressReporter(_progressFlow, _completed + completedDelta, _target - targetDelta)
    }

    companion object {
        const val UNREPORTED = -1
        const val ERROR = -2
    }
}

inline fun <R> trackProgressWith(
    startProgress: Int, endProgress: Int,
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

    return trackProgressWith(startProgress = 0, endProgress = 100, reporter, block)
}

inline fun trackLoopProgressWith(reporter: ProgressReporter?, size: Int, loopBody: (index: Int) -> Unit) {
    contract {
        callsInPlace(loopBody, InvocationKind.UNKNOWN)
    }

    trackProgressWith(reporter) {
        for (i in 0 until size) {
            loopBody(i)
            reporter?.onProgress(i + 1, size)
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

    trackProgressWith(reporter) {
        for (i in 0 until size) {
            val iMulSize = i * size
            val subReporter = reporter?.subReporter(iMulSize / 100, (iMulSize + size) / 100)

            loopBody(i, subReporter)
        }
    }
}