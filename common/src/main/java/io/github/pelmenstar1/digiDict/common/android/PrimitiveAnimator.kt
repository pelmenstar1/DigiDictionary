package io.github.pelmenstar1.digiDict.common.android

import android.view.Choreographer
import io.github.pelmenstar1.digiDict.common.time.NANOSECONDS_IN_MILLISECOND
import kotlin.math.max

class PrimitiveAnimator(private val callback: AnimationTickCallback) {
    private var durationNs = 0L
    var duration = 0L
        set(value) {
            field = value
            durationNs = value * NANOSECONDS_IN_MILLISECOND
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