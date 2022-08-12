package io.github.pelmenstar1.digiDict.common

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class ProgressReporter {
    private val _progressFlow: MutableStateFlow<Int>
    val progressFlow: StateFlow<Int>

    private val _completed: Int
    private val _target: Int
    private val _diff: Int

    constructor() {
        _progressFlow = MutableStateFlow(UNREPORTED)
        progressFlow = _progressFlow.asStateFlow()

        _completed = 0
        _target = 100
        _diff = 100
    }

    private constructor(pFlow: MutableStateFlow<Int>, completed: Int, target: Int) {
        _progressFlow = pFlow
        progressFlow = pFlow.asStateFlow()

        _completed = completed
        _target = target
        _diff = target - completed
    }

    fun onProgress(current: Int, total: Int) {
        if (current > total) {
            throw IllegalArgumentException("current > total")
        }

        _progressFlow.value = _completed + (current * _diff) / total
    }

    fun onEnd() {
        _progressFlow.value = _target
    }

    fun reset() {
        _progressFlow.value = UNREPORTED
    }

    fun subReporter(completed: Int, target: Int): ProgressReporter {
        if (completed < 0 || completed >= 100) {
            throw IllegalArgumentException("completed is negative or greater than 100")
        }

        if (target <= 0 || target > 100) {
            throw IllegalArgumentException("target is negative or greater than 100")
        }

        if (completed >= target) {
            throw IllegalArgumentException("completed >= target")
        }

        return ProgressReporter(_progressFlow, completed, target)
    }

    companion object {
        const val UNREPORTED = -1
    }
}