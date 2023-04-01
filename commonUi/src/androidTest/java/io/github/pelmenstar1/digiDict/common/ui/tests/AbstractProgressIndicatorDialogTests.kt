package io.github.pelmenstar1.digiDict.common.ui.tests

import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ProgressBar
import androidx.fragment.app.testing.launchFragmentInContainer
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.android.material.progressindicator.CircularProgressIndicator
import io.github.pelmenstar1.digiDict.common.ui.AbstractProgressIndicatorDialog
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

@RunWith(AndroidJUnit4::class)
class AbstractProgressIndicatorDialogTests {
    class Impl : AbstractProgressIndicatorDialog() {
        override fun createLayout(): Pair<ProgressBar, ViewGroup> {
            val context = requireContext()

            val progressIndicator = CircularProgressIndicator(context).apply {
                isIndeterminate = false
            }

            val container = FrameLayout(context).apply {
                addView(progressIndicator)
            }

            return progressIndicator to container
        }
    }

    private fun assertProgress(expectedProgress: Int, dialog: AbstractProgressIndicatorDialog) {
        val progressBar = dialog.progressBar

        assertNotNull(progressBar, "progressBar is null")
        assertEquals(expectedProgress, progressBar.progress, "unexpected progress")
    }

    @Test
    fun recreateTest() {
        val expectedProgress = 50

        val scenario = launchFragmentInContainer<Impl>(themeResId = R.style.Theme_Material3_Dark)
        scenario.onFragment {
            it.setProgress(expectedProgress)

            assertProgress(expectedProgress, it)
        }

        scenario.recreate()

        scenario.onFragment {
            assertProgress(expectedProgress, it)
        }
    }

}