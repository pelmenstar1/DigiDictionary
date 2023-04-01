package io.github.pelmenstar1.digiDict.common.ui.tests

import androidx.fragment.app.Fragment
import androidx.fragment.app.testing.FragmentScenario
import androidx.fragment.app.testing.launchFragmentInContainer
import androidx.test.espresso.Espresso.*
import androidx.test.espresso.ViewInteraction
import androidx.test.espresso.action.ViewActions.*
import androidx.test.espresso.assertion.ViewAssertions.*
import androidx.test.espresso.matcher.RootMatchers.*
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.android.material.checkbox.MaterialCheckBox
import io.github.pelmenstar1.digiDict.common.ui.selectionDialogs.ChoicesProvider
import io.github.pelmenstar1.digiDict.common.ui.selectionDialogs.MultiSelectionDialogFragment
import io.github.pelmenstar1.digiDict.common.ui.tests.R
import org.hamcrest.Matchers.*
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.test.assertContentEquals

@RunWith(AndroidJUnit4::class)
class MultiSelectionDialogFragmentTests {
    private class CapturedSelectedValues {
        var array: Array<String>? = null
    }

    open class Impl : MultiSelectionDialogFragment<String>() {
        override val choices: ChoicesProvider
            get() = stringArrayResource(R.array.test_singleSelectionDialogFragment_choices)

        override val titleRes: Int
            get() = R.string.test_singleSelectionDialogFragment_title

        override fun getValueByIndex(index: Int): String = index.toString()

        override fun createValueArray(size: Int): Array<String?> = arrayOfNulls(size)
    }

    class AtLeastOneSelectedImpl : Impl() {
        override val atLeastOneShouldBeSelected: Boolean
            get() = true
    }

    private inline fun <reified F : Fragment> launchDialog(vararg selectedIndices: Int): FragmentScenario<F> {
        return launchFragmentInContainer(
            themeResId = com.google.android.material.R.style.Theme_Material3_Dark,
            fragmentArgs = MultiSelectionDialogFragment.createArguments(selectedIndices)
        )
    }

    private fun ViewInteraction.checkIsCheckBoxAndDisplayed(): ViewInteraction {
        return check(matches(instanceOf(MaterialCheckBox::class.java)))
            .check(matches(isDisplayed()))
    }

    private fun onNoOptionSelectionError(): ViewInteraction {
        return onView(withText(R.string.multiSelectionDialog_noOptionSelectedError)).inRoot(isDialog())
    }

    private fun onChoice(index: Int): ViewInteraction {
        return onView(withText("Choice ${index + 1}")).inRoot(isDialog())
    }

    private fun onApplyButton(): ViewInteraction {
        return onView(withText(R.string.multiSelectionDialog_apply)).inRoot(isDialog())
    }

    private fun checkNoOptionSelectionErrorDisplayedAndApplyDisabled() {
        onNoOptionSelectionError().check(matches(isDisplayed()))
        onApplyButton().check(matches(isNotEnabled()))
    }

    private fun checkNoOptionSelectionErrorDoesNotExistAndApplyEnabled() {
        onNoOptionSelectionError().check(matches(withEffectiveVisibility(Visibility.INVISIBLE)))
        onApplyButton().check(matches(isEnabled()))
    }

    private fun <F : MultiSelectionDialogFragment<String>> captureSelectedValues(scenario: FragmentScenario<F>): CapturedSelectedValues {
        val obj = CapturedSelectedValues()

        scenario.onFragment {
            it.onValuesSelected = { values -> obj.array = values }
        }

        return obj
    }

    @Test
    fun initialLayoutTest() {
        launchDialog<Impl>(0, 2)

        onView(withText("Title")).inRoot(isDialog()).check(matches(isDisplayed()))

        onChoice(index = 0)
            .checkIsCheckBoxAndDisplayed()
            .check(matches(isChecked()))

        onChoice(index = 1).checkIsCheckBoxAndDisplayed()

        onChoice(index = 2)
            .checkIsCheckBoxAndDisplayed()
            .check(matches(isChecked()))

        onApplyButton()
            .check(matches(isDisplayed()))
            .check(matches(isEnabled()))

        onNoOptionSelectionError().check(matches(isDisplayed()))
    }

    @Test
    fun initialLayoutOnNoSelectionTest() {
        launchDialog<AtLeastOneSelectedImpl>()

        checkNoOptionSelectionErrorDisplayedAndApplyDisabled()
    }

    @Test
    fun dynamicNoSelectionTest() {
        launchDialog<AtLeastOneSelectedImpl>(1)

        checkNoOptionSelectionErrorDoesNotExistAndApplyEnabled()
        onChoice(index = 1).perform(click())

        checkNoOptionSelectionErrorDisplayedAndApplyDisabled()

        onChoice(index = 0).perform(click())
        checkNoOptionSelectionErrorDoesNotExistAndApplyEnabled()
    }

    @Test
    fun applyNoActionTest() {
        fun testCase(selectedIndices: IntArray) {
            val scenario = launchDialog<Impl>(*selectedIndices)
            val selectedValues = captureSelectedValues(scenario)

            onApplyButton().perform(click())

            val expectedSelectedValues = selectedIndices.map { it.toString() }.toTypedArray()
            assertContentEquals(expectedSelectedValues, selectedValues.array)
        }

        testCase(selectedIndices = intArrayOf(1, 2))
        testCase(selectedIndices = intArrayOf(0, 1, 2))
        testCase(selectedIndices = intArrayOf())
    }

    @Test
    fun applyWithActionTest() {
        val scenario = launchDialog<Impl>(0)
        val selectedValues = captureSelectedValues(scenario)

        onChoice(index = 0).perform(click()) // Choice 0 is disabled
        onChoice(index = 1).perform(click()) // Choice 1 is enabled
        onChoice(index = 2).perform(click()) // Choice 2 is enabled
        onChoice(index = 0).perform(click()) // Choice 3 is enabled

        onApplyButton().perform(click())

        assertContentEquals(arrayOf("0", "1", "2"), selectedValues.array)
    }

    private fun validateCheckStates(selectedIndices: IntArray) {
        for (i in 0 until 3) {
            val matcher = if (selectedIndices.contains(i)) isChecked() else isNotChecked()

            onChoice(i).check(matches(matcher))
        }
    }

    @Test
    fun recreateWithNoActionTest() {
        fun testCase(selectedIndices: IntArray) {
            val scenario = launchDialog<Impl>(*selectedIndices)
            scenario.recreate()

            validateCheckStates(selectedIndices)
        }

        testCase(selectedIndices = intArrayOf())
        testCase(selectedIndices = intArrayOf(1, 2))
        testCase(selectedIndices = intArrayOf(0, 1, 2))
    }

    @Test
    fun recreateWithActionTest() {
        val scenario = launchDialog<Impl>(1, 2)

        onChoice(index = 1).perform(click())
        onChoice(index = 0).perform(click())

        scenario.recreate()

        validateCheckStates(selectedIndices = intArrayOf(0, 2))
    }
}