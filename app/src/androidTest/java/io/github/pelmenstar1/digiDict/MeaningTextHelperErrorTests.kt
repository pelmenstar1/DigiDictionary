package io.github.pelmenstar1.digiDict

import android.content.res.Configuration
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import io.github.pelmenstar1.digiDict.ui.MeaningTextHelper
import org.junit.Test
import org.junit.runner.RunWith
import java.util.*
import kotlin.test.assertEquals

@RunWith(AndroidJUnit4::class)
class MeaningTextHelperErrorTests {
    private val testContext = InstrumentationRegistry.getInstrumentation().context
    private val targetContext = InstrumentationRegistry.getInstrumentation().targetContext

    @Test
    fun getErrorMessageForFormatExceptionOnTestContextTest() {
        val actualMsg = MeaningTextHelper.getErrorMessageForFormatException(testContext, Exception("test message"))
        val expectedMsg = "An error happened:\ntest message"

        assertEquals(expectedMsg, actualMsg)
    }

    @Test
    fun getErrorMessageForFormatExceptionOnTargetContextTest() {
        val currentConf = targetContext.resources.configuration
        val newConf = Configuration(currentConf).apply {
            setLocale(Locale.forLanguageTag("en-US"))
        }

        val englishLocaleContext = targetContext.createConfigurationContext(newConf)
        val actualMsg =
            MeaningTextHelper.getErrorMessageForFormatException(englishLocaleContext, Exception("test message"))
        val expectedMsg = "An error happened:\ntest message"

        assertEquals(expectedMsg, actualMsg)
    }
}