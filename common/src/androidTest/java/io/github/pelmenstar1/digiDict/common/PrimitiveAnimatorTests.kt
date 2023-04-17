package io.github.pelmenstar1.digiDict.common

import android.os.Looper
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.pelmenstar1.digiDict.common.android.PrimitiveAnimator
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.CountDownLatch
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@RunWith(AndroidJUnit4::class)
class PrimitiveAnimatorTests {
    private class AnimatorThread(
        private val createAnimator: () -> PrimitiveAnimator,
        private val isForward: Boolean
    ) : Thread() {
        private var looper: Looper? = null

        fun quitLooper() {
            looper!!.quitSafely()
        }

        override fun run() {
            Looper.prepare()
            looper = Looper.myLooper()

            createAnimator().also {
                it.duration = ANIMATION_DURATION
                it.start(isForward)
            }

            Looper.loop()
        }
    }

    private fun startAnimatorThread(
        createAnimator: () -> PrimitiveAnimator,
        isForward: Boolean
    ): AnimatorThread {
        return AnimatorThread(createAnimator, isForward).also { it.start() }
    }

    private fun firstFractionShouldBeTestHelper(isForward: Boolean, expectedValue: Float) {
        var isFirst = true
        val latch = CountDownLatch(1)

        val thread = startAnimatorThread(
            createAnimator = {
                PrimitiveAnimator { fraction ->
                    if (isFirst) {
                        isFirst = false

                        assertEquals(expectedValue, fraction)
                        latch.countDown()
                    }
                }
            },
            isForward
        )

        latch.await()
        thread.quitLooper()
    }

    @Test
    fun firstFractionShouldBeZeroOnForwardTest() {
        firstFractionShouldBeTestHelper(isForward = true, expectedValue = 0f)
    }

    @Test
    fun firstFractionShouldBeOneOnBackwardTest() {
        firstFractionShouldBeTestHelper(isForward = false, expectedValue = 1f)
    }

    private fun lastFractionShouldBeTestHelper(isForward: Boolean, expectedValue: Float) {
        var lastFraction = Float.NaN
        val thread = startAnimatorThread(
            createAnimator = {
                PrimitiveAnimator { fraction ->
                    lastFraction = fraction
                }
            },
            isForward
        )

        // As we cannot check whether the animation actually ended we wait doubled time of animation
        // and then check the lastFraction
        Thread.sleep(ANIMATION_DURATION * 2)
        thread.quitLooper()

        assertEquals(expectedValue, lastFraction)
    }

    @Test
    fun lastFractionShouldBeOneOnForwardTest() {
        lastFractionShouldBeTestHelper(isForward = true, expectedValue = 1f)
    }

    @Test
    fun lastFractionShouldBeZeroOnBackwardTest() {
        lastFractionShouldBeTestHelper(isForward = false, expectedValue = 0f)
    }

    private fun fractionsShouldBeInRangeZeroOneTestHelper(isForward: Boolean) {
        val thread = startAnimatorThread(
            createAnimator = {
                PrimitiveAnimator { fraction ->
                    assertTrue(fraction in 0f..1f, "fraction: $fraction")
                }
            },
            isForward
        )

        // Keep the test alive.
        Thread.sleep(ANIMATION_DURATION * 2)

        // To stop the animator thread.
        thread.quitLooper()
    }

    @Test
    fun fractionsShouldBeInRangeZeroOneOnForwardTest() {
        fractionsShouldBeInRangeZeroOneTestHelper(isForward = true)
    }

    @Test
    fun fractionsShouldBeInRangeZeroOneOnBackwardTest() {
        fractionsShouldBeInRangeZeroOneTestHelper(isForward = false)
    }

    companion object {
        private const val ANIMATION_DURATION = 100L
    }
}