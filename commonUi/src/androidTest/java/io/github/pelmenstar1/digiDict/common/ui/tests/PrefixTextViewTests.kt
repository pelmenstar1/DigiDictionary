package io.github.pelmenstar1.digiDict.common.ui.tests

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.pelmenstar1.digiDict.common.ui.PrefixTextView
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.test.assertEquals

@RunWith(AndroidJUnit4::class)
class PrefixTextViewTests {
    private val context = ApplicationProvider.getApplicationContext<Context>()

    @Test
    fun setValueTextTest() {
        val view = PrefixTextView(context)

        fun testCase(prefix: String, value: String, expectedText: String) {
            view.prefix = prefix
            view.setValue(value)

            assertEquals(expectedText, view.text.toString())
        }

        testCase(prefix = "Prefix", value = "", expectedText = "Prefix: ")
        testCase(prefix = "Prefix", value = "123", expectedText = "Prefix: 123")
        testCase(prefix = "Prefix", value = "Value", expectedText = "Prefix: Value")

        testCase(prefix = "", value = "", expectedText = ": ")
        testCase(prefix = "", value = "123", expectedText = ": 123")
        testCase(prefix = "", value = "1", expectedText = ": 1")

        testCase(prefix = "P", value = "", expectedText = "P: ")
        testCase(prefix = "P", value = "123", expectedText = "P: 123")
        testCase(prefix = "P", value = "Value", expectedText = "P: Value")
    }

    @Test
    fun setValueNumberTest() {
        val view = PrefixTextView(context)
        view.text = "Prefix"

        fun testCase(prefix: String, value: Int, expectedText: String) {
            view.prefix = prefix
            view.setValue(value)

            assertEquals(expectedText, view.text.toString())
        }

        testCase(prefix = "Prefix", value = 1, expectedText = "Prefix: 1")
        testCase(prefix = "Prefix", value = 0, expectedText = "Prefix: 0")
        testCase(prefix = "Prefix", value = 123, expectedText = "Prefix: 123")
        testCase(prefix = "Prefix", value = -100, expectedText = "Prefix: -100")

        testCase(prefix = "", value = 1, expectedText = ": 1")
        testCase(prefix = "", value = 0, expectedText = ": 0")
        testCase(prefix = "", value = 123, expectedText = ": 123")
        testCase(prefix = "", value = -100, expectedText = ": -100")

        testCase(prefix = "P", value = 1, expectedText = "P: 1")
        testCase(prefix = "P", value = 0, expectedText = "P: 0")
        testCase(prefix = "P", value = 123, expectedText = "P: 123")
        testCase(prefix = "P", value = -100, expectedText = "P: -100")
    }

    @Test
    fun valueIsChangedWhenPrefixIsChangedTest() {
        val view = PrefixTextView(context)
        view.setValue("123")

        fun testCase(newPrefix: String, expectedText: String) {
            view.prefix = newPrefix

            assertEquals(expectedText, view.text.toString())
        }

        testCase(newPrefix = "P", expectedText = "P: 123")
        testCase(newPrefix = "", expectedText = ": 123")
        testCase(newPrefix = "Prefix", expectedText = "Prefix: 123")
    }
}