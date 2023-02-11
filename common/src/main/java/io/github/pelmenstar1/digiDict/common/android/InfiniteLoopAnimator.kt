package io.github.pelmenstar1.digiDict.common.android

import android.view.Choreographer
import io.github.pelmenstar1.digiDict.common.time.NANOSECONDS_IN_MILLISECOND
import kotlin.math.max

/**
 * Responsible for handling basic state of a infinite looped animation.
 */
class InfiniteLoopAnimator(private val callback: AnimationTickCallback) {
    private var durationNs: Long = 0L
    private var loopStartTime: Long = 0L
    private var isForward: Boolean = false
    private var _isStarted: Boolean = false

    private val choreographer = Choreographer.getInstance()
    private val frameCallback = Choreographer.FrameCallback(::onFrame)

    /**
     * Gets or sets the duration of a loop of the animation in milliseconds.
     */
    var duration: Long = 0L
        set(value) {
            require(value >= 0) { "Duration is expected to be non-negative" }

            field = value
            durationNs = value * NANOSECONDS_IN_MILLISECOND
        }

    /**
     * Determines whether the animation is started.
     */
    val isStarted: Boolean
        get() = _isStarted

    /**
     * Starts the animation.
     *
     * If the animation is started already, does nothing.
     */
    fun start() {
        if (_isStarted) {
            return
        }

        _isStarted = true
        loopStartTime = System.nanoTime()
        callback.onTick(0f)

        choreographer.postFrameCallback(frameCallback)

    }

    /**
     * Stops the animation and executes the callback with `0` fraction.
     *
     * If the animation is not started, does nothing.
     */
    fun stop() {
        if (!_isStarted) {
            return
        }

        callback.onTick(0f)
        _isStarted = false
    }

    private fun onFrame(time: Long) {
        if (!_isStarted) {
            return
        }

        // Sometimes, time is greater than loopStartTime for some reason.
        val elapsed = max(0, time - loopStartTime)
        val dur = durationNs
        var forward = isForward
        var fraction = 0f

        if (elapsed >= dur) {
            loopStartTime = time
            forward = !forward
            isForward = forward

            if (!forward) {
                fraction = 1f
            }
        } else {
            var elapsedBasedOnForward = elapsed

            if (!forward) {
                elapsedBasedOnForward = dur - elapsed
            }

            fraction = elapsedBasedOnForward / dur.toFloat()
        }

        callback.onTick(fraction)
        choreographer.postFrameCallback(frameCallback)
    }

}