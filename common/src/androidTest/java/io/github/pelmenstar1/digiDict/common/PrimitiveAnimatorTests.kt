package io.github.pelmenstar1.digiDict.common

import android.app.Activity
import android.os.Bundle
import android.os.PersistableBundle
import android.view.View
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.pelmenstar1.digiDict.common.android.PrimitiveAnimator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@RunWith(AndroidJUnit4::class)
class PrimitiveAnimatorTests {
    class TestActivity : Activity() {
        override fun onCreate(savedInstanceState: Bundle?, persistentState: PersistableBundle?) {
            super.onCreate(savedInstanceState, persistentState)

            setContentView(View(this))
        }
    }

    private inline fun testHelper(crossinline block: () -> Unit) {
        ActivityScenario.launch(TestActivity::class.java).use { scenerio ->
            scenerio.onActivity {
                block()
            }
        }
    }

    private fun firstFractionShouldBeTestHelper(isForward: Boolean, expectedValue: Float) {
        testHelper {
            var isFirst = true
            val animator = PrimitiveAnimator { fraction ->
                if (isFirst) {
                    isFirst = false

                    assertEquals(expectedValue, fraction)
                }
            }
            animator.duration = ANIMATION_DURATION

            animator.start(isForward)
        }
    }

    @Test
    fun firstFractionShouldBeZeroOnForwardTest() {
        firstFractionShouldBeTestHelper(isForward = true, expectedValue = 0f)
    }

    @Test
    fun firstFractionShouldBeOneOnBackwardTest() {
        firstFractionShouldBeTestHelper(isForward = false, expectedValue = 1f)
    }

    private fun lastFractionShouldBeTestHelper(isForward: Boolean, expectedValue: Float) = runBlocking {
        testHelper {
            var lastFraction = Float.NaN
            val animator = PrimitiveAnimator { fraction ->
                lastFraction = fraction
            }
            animator.duration = ANIMATION_DURATION

            animator.start(isForward)

            launch(Dispatchers.Default) {
                // As we cannot check whether the animation actually ended we wait doubled time of animation
                // and then check the lastFraction
                delay(ANIMATION_DURATION * 2)

                assertEquals(expectedValue, lastFraction)
            }
        }
    }

    @Test
    fun lastFractionShouldBeOneOnForwardTest() {
        lastFractionShouldBeTestHelper(isForward = true, expectedValue = 1f)
    }

    @Test
    fun lastFractionShouldBeZeroOnBackwardTest() {
        lastFractionShouldBeTestHelper(isForward = false, expectedValue = 0f)
    }

    private fun fractionsShouldBeInRangeZeroOneTestHelper(isForward: Boolean) = runBlocking {
        testHelper {
            val animator = PrimitiveAnimator { fraction ->
                assertTrue(fraction in 0f..1f, "fraction: $fraction")
            }
            animator.duration = ANIMATION_DURATION

            animator.start(isForward)

            launch(Dispatchers.Default) {
                // Keep the test alive
                delay(ANIMATION_DURATION * 2)
            }
        }
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