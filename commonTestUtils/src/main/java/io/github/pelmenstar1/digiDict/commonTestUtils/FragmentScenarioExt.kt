package io.github.pelmenstar1.digiDict.commonTestUtils

import androidx.fragment.app.Fragment
import androidx.fragment.app.testing.FragmentScenario
import kotlin.test.assertFails

fun <F : Fragment> assertFragmentNotAttached(scenario: FragmentScenario<F>) {
    // onFragment() fails if the fragment is detached from FragmentManager. Seems it's the only way
    // to check whether the dialog has been dismissed without rewriting FragmentScenario.
    assertFails { scenario.onFragment {} }
}