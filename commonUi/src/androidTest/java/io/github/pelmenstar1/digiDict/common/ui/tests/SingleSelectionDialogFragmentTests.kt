package io.github.pelmenstar1.digiDict.common.ui.tests

import androidx.fragment.app.testing.FragmentScenario
import androidx.fragment.app.testing.launchFragmentInContainer
import androidx.test.espresso.Espresso.*
import androidx.test.espresso.ViewInteraction
import androidx.test.espresso.action.ViewActions.*
import androidx.test.espresso.assertion.ViewAssertions.*
import androidx.test.espresso.matcher.RootMatchers.*
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.android.material.radiobutton.MaterialRadioButton
import io.github.pelmenstar1.digiDict.common.ui.selectionDialogs.SingleSelectionDialogFragment
import io.github.pelmenstar1.digiDict.common.ui.tests.R
import io.github.pelmenstar1.digiDict.commonTestUtils.assertFragmentNotAttached
import org.hamcrest.Matchers.*
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.test.assertEquals

@RunWith(AndroidJUnit4::class)
class SingleSelectionDialogFragmentTests {
    class SingleSelectionDialogFragmentImpl : SingleSelectionDialogFragment<String>() {
        override val choicesRes: Int
            get() = R.array.test_singleSelectionDialogFragment_choices

        override val titleRes: Int
            get() = R.string.test_singleSelectionDialogFragment_title

        override fun getValueByIndex(index: Int): String = index.toString()
    }

    private fun launchDialog(selectedIndex: Int): FragmentScenario<SingleSelectionDialogFragmentImpl> {
        return launchFragmentInContainer(
            themeResId = com.google.android.material.R.style.Theme_Material3_Dark,
            fragmentArgs = SingleSelectionDialogFragment.createArguments(selectedIndex)
        )
    }

    private fun ViewInteraction.checkRadioButtonAndDisplayed(): ViewInteraction {
        return check(matches(instanceOf(MaterialRadioButton::class.java)))
            .check(matches(isDisplayed()))
    }

    private fun onViewWithText(text: String): ViewInteraction {
        return onView(withText(text)).inRoot(isDialog())
    }

    @Test
    fun layoutTest() {
        launchDialog(selectedIndex = 1)

        onViewWithText("Title").check(matches(isDisplayed()))
        onViewWithText("Choice 1").checkRadioButtonAndDisplayed()
        onViewWithText("Choice 2").checkRadioButtonAndDisplayed().check(matches(isChecked()))
        onViewWithText("Choice 3").checkRadioButtonAndDisplayed()
    }

    @Test
    fun chooseTest() {
        val scenario = launchDialog(selectedIndex = 1)

        var selectedValue: String? = null
        scenario.onFragment {
            it.onValueSelected = { value -> selectedValue = value }
        }

        onViewWithText("Choice 3").perform(click())
        assertEquals("2", selectedValue)

        assertFragmentNotAttached(scenario)
    }
}