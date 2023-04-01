package io.github.pelmenstar1.digiDict.common.ui.tests

import androidx.fragment.app.Fragment
import androidx.fragment.app.testing.FragmentScenario
import androidx.fragment.app.testing.launchFragmentInContainer
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.pelmenstar1.digiDict.common.ProgressReporter
import io.github.pelmenstar1.digiDict.common.ui.AbstractProgressIndicatorDialog
import io.github.pelmenstar1.digiDict.common.ui.SimpleProgressIndicatorDialog
import io.github.pelmenstar1.digiDict.common.ui.SimpleProgressIndicatorDialogManager
import io.github.pelmenstar1.digiDict.commonTestUtils.onFragmentBlocking
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.receiveAsFlow
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotNull

@RunWith(AndroidJUnit4::class)
class SimpleProgressIndicatorDialogManagerTests {
    class HostFragment : Fragment()

    private fun launchHost(): FragmentScenario<HostFragment> {
        return launchFragmentInContainer(themeResId = R.style.Theme_Material3_Dark)
    }

    private fun dialogShouldNotBeShownIfFirstProgressTestHelper(firstProgress: Int) {
        val manager = SimpleProgressIndicatorDialogManager()

        launchHost().onFragment {
            manager.init(it, flowOf(firstProgress))
            manager.showDialog()

            assertEquals(0, it.childFragmentManager.fragments.size)
        }
    }

    @Test
    fun dialogShouldNotBeShownIfFirstProgressIsErrorTest() {
        dialogShouldNotBeShownIfFirstProgressTestHelper(ProgressReporter.ERROR)
    }

    @Test
    fun dialogShouldNotBeShownIfFirstProgressIsUnreportedTest() {
        dialogShouldNotBeShownIfFirstProgressTestHelper(ProgressReporter.UNREPORTED)
    }

    @Test
    fun dialogShouldNotBeShownIfFirstProgressIs100Test() {
        dialogShouldNotBeShownIfFirstProgressTestHelper(firstProgress = 100)
    }

    private fun assertProgress(expectedProgress: Int, dialog: AbstractProgressIndicatorDialog) {
        val progressBar = dialog.progressBar

        assertNotNull(progressBar, "progressBar is null")
        assertEquals(expectedProgress, progressBar.progress, "unexpected progress")
    }

    @Test
    fun progressTest() {
        val manager = SimpleProgressIndicatorDialogManager()
        val progressChannel = Channel<Int>(Channel.UNLIMITED)

        launchHost().onFragmentBlocking {
            manager.init(it, progressChannel.receiveAsFlow())
            manager.showDialog()

            progressChannel.send(1)
            var fragments = it.childFragmentManager.fragments
            assertEquals(1, fragments.size, "childFragmentManager is expected to contain only the progress dialog")

            val dialog = fragments[0]
            assertIs<SimpleProgressIndicatorDialog>(dialog)

            assertProgress(expectedProgress = 1, dialog)

            progressChannel.send(50)
            assertProgress(expectedProgress = 50, dialog)

            progressChannel.send(100)

            fragments = it.childFragmentManager.fragments
            assertEquals(
                0,
                fragments.size,
                "childFragmentManager is expected to have no fragments after progress 100"
            )
        }
    }

    @Test
    fun recreateTest() {
        val manager = SimpleProgressIndicatorDialogManager()
        val progressChannel = Channel<Int>(Channel.UNLIMITED)
        val progressFlow = progressChannel.receiveAsFlow()

        val scenario = launchHost()
        scenario.onFragmentBlocking { fragment ->
            manager.init(fragment, progressFlow)
            manager.showDialog()

            progressChannel.send(5)
        }

        scenario.recreate()

        scenario.onFragmentBlocking { fragment ->
            manager.init(fragment, progressFlow)

            val fragments = fragment.childFragmentManager.fragments

            // Testing whether the logic found out that there's already the dialog shown.
            assertEquals(1, fragments.size, "childFragmentManager is expected only to contain the progress dialog")

            val dialog = fragments[0]
            assertIs<SimpleProgressIndicatorDialog>(dialog)

            assertProgress(expectedProgress = 5, dialog)
        }
    }
}