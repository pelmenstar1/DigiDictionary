package io.github.pelmenstar1.digiDict.commonTestUtils

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.testing.launchFragmentInContainer
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.test.assertFails

@RunWith(AndroidJUnit4::class)
class FragmentScenarioExtTests {
    class TestFragment : Fragment() {
        override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
            return View(requireContext())
        }

        fun detachSelf() {
            parentFragmentManager.beginTransaction().remove(this).commitNow()
        }
    }

    @Test
    fun assertFragmentNotAttachedTest() {
        val scenario = launchFragmentInContainer<TestFragment>()

        // The fragment is attached right now.
        assertFails { assertFragmentNotAttached(scenario) }
        scenario.onFragment { it.detachSelf() }

        // Now the fragment is detached
        assertFragmentNotAttached(scenario)
    }
}