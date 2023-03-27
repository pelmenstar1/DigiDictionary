package io.github.pelmenstar1.digiDict.common.ui

import android.os.Bundle
import android.view.ViewGroup
import android.widget.Button
import android.widget.FrameLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.iterator
import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso.*
import androidx.test.espresso.action.ViewActions.*
import androidx.test.espresso.assertion.ViewAssertions.*
import androidx.test.espresso.matcher.RootMatchers.*
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

@RunWith(AndroidJUnit4::class)
class OptionsBarTests {
    class TestActivity : AppCompatActivity() {
        lateinit var optionsBar: OptionsBar

        override fun onCreate(savedInstanceState: Bundle?) {
            super.onCreate(savedInstanceState)

            setContentView(FrameLayout(this).apply {
                optionsBar = OptionsBar(context)

                addView(optionsBar)
            })
        }
    }

    private val option1 = OptionsBar.Option(id = ID_1, prefixRes = R.string.test_optionsBar_prefix1)
    private val option2 = OptionsBar.Option(id = ID_2, prefixRes = R.string.test_optionsBar_prefix2)
    private val option3 = OptionsBar.Option(id = ID_3, prefixRes = R.string.test_optionsBar_prefix3)

    private val preset1 = OptionsBar.Preset(option1, option2)
    private val preset2 = OptionsBar.Preset(option1, option2, option3)

    private fun launchActivity(): ActivityScenario<TestActivity> {
        return ActivityScenario.launch(TestActivity::class.java)
    }

    private fun validateButtonTexts(optionsBar: OptionsBar, texts: Array<String>) {
        val container = optionsBar.getTypedViewAt<ViewGroup>(0)
        assertEquals(texts.size, container.childCount, "Invalid button count")

        for ((i, button) in container.iterator().withIndex()) {
            assertIs<Button>(button)
            assertEquals(texts[i], button.text)
        }
    }

    @Test
    fun contentTest() {
        val scenario = launchActivity()
        scenario.onActivity {
            it.optionsBar.apply {
                setPreset(preset2)

                setOptionValue(ID_1, "Value1")
                setOptionValue(ID_2, "Value2")
                setOptionValue(ID_3, "")

                validateButtonTexts(this, arrayOf("Prefix1: Value1", "Prefix2: Value2", "Prefix3: "))
            }
        }
    }

    @Test
    fun setPresetSavesPreviousValuesTest() {
        val scenario = launchActivity()
        scenario.onActivity {
            it.optionsBar.apply {
                setPreset(preset2)

                setOptionValue(ID_1, "Value1")
                setOptionValue(ID_2, "Value2")
                setOptionValue(ID_3, "Value3")

                setPreset(preset1)
                validateButtonTexts(this, arrayOf("Prefix1: Value1", "Prefix2: Value2"))

                setPreset(preset2)
                validateButtonTexts(this, arrayOf("Prefix1: Value1", "Prefix2: Value2", "Prefix3: Value3"))
            }
        }
    }

    @Test
    fun buttonClickTest() {
        var isOnClickListenerCalled = false

        val scenario = launchActivity()
        scenario.onActivity {
            it.optionsBar.apply {
                setPreset(preset1)

                setOptionOnClickListener(ID_1) { isOnClickListenerCalled = true }
            }
        }

        onView(withId(ID_1)).perform(click())

        assertTrue(isOnClickListenerCalled)
    }

    companion object {
        private const val ID_1 = 101
        private const val ID_2 = 102
        private const val ID_3 = 103
    }
}