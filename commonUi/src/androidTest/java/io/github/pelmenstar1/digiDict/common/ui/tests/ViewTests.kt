package io.github.pelmenstar1.digiDict.common.ui.tests

import android.view.View
import android.widget.LinearLayout
import androidx.core.view.get
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import io.github.pelmenstar1.digiDict.common.ui.adjustViewCount
import io.github.pelmenstar1.digiDict.common.ui.adjustViewCountWithoutLast
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.test.assertEquals

@RunWith(AndroidJUnit4::class)
class ViewTests {
    private fun adjustViewCountTestHelper(testCase: (current: Int, adjusted: Int) -> Unit) {
        // Idempotence
        testCase(0, 0)
        testCase(1, 1)
        testCase(2, 2)

        // When there's no views in the start and we want to add some
        testCase(0, 1)
        testCase(0, 5)

        // When there's some views and we want to add more
        testCase(5, 6)
        testCase(1, 2)
        testCase(2, 10)

        // When there's some views and we want to remove all the views
        testCase(1, 0)
        testCase(5, 0)

        // When there's some views and we want to remove some
        testCase(5, 1)
        testCase(5, 4)
        testCase(2, 1)
    }

    @Test
    fun adjustViewCountTest() {
        fun testCase(current: Int, adjusted: Int) {
            val context = InstrumentationRegistry.getInstrumentation().context
            val layout = LinearLayout(context)
            repeat(current) {
                layout.addView(View(context))
            }

            layout.adjustViewCount(targetCount = adjusted) { addView(View(context)) }

            assertEquals(adjusted, layout.childCount)
        }

        adjustViewCountTestHelper(::testCase)
    }

    @Test
    fun adjustViewCountWithoutLastTest() {
        fun testCase(current: Int, adjusted: Int) {
            val context = InstrumentationRegistry.getInstrumentation().context
            val layout = LinearLayout(context)
            repeat(current) {
                layout.addView(View(context))
            }

            val specialView = View(context)
            layout.addView(specialView)

            layout.adjustViewCountWithoutLast(targetCount = adjusted, lastViewsCount = 1) {
                addView(View(context), 0)
            }

            assertEquals(adjusted + 1, layout.childCount)

            // To ensure adjustViewCountWithoutLast doesn't touch the last views
            assertEquals(specialView, layout[layout.childCount - 1])
        }

        adjustViewCountTestHelper(::testCase)
    }


}