package io.github.pelmenstar1.digiDict.common.android

import android.view.Choreographer
import kotlin.math.max

class PrimitiveAnimator(private val callback: TickCallback) {
    fun interface TickCallback {
        fun onTick(fraction: Float)
    }

    private var durationNs = 0L
    var duration = 0L
        set(value) {
            field = value
            durationNs = value * 1_000_000
        }

    private var _isRunning = false
    val isRunning: Boolean
        get() = _isRunning

    private var startTime = 0L
    private var isForward = false

    private val frameCallback = Choreographer.FrameCallback(::onFrame)
    private val choreographer = Choreographer.getInstance()

    fun start(forward: Boolean = true) {
        _isRunning = true
        isForward = forward
        startTime = System.nanoTime()
        callback.onTick(if (forward) 0f else 1f)

        choreographer.postFrameCallback(frameCallback)
    }

    private fun onFrame(time: Long) {
        val elapsed = max(0, time - startTime)

        if (elapsed < durationNs) {
            var fraction = elapsed / durationNs.toFloat()

            if (!isForward) {
                fraction = 1f - fraction
            }

            callback.onTick(fraction)
            choreographer.postFrameCallback(frameCallback)
        } else {
            _isRunning = false

            callback.onTick(if (isForward) 1f else 0f)
        }
    }
}