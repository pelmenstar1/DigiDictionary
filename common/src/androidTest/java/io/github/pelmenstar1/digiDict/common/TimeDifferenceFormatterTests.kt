package io.github.pelmenstar1.digiDict.common

import android.content.res.Configuration
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import io.github.pelmenstar1.digiDict.common.time.SECONDS_IN_DAY
import io.github.pelmenstar1.digiDict.common.time.SECONDS_IN_HOUR
import io.github.pelmenstar1.digiDict.common.time.SECONDS_IN_WEEK
import io.github.pelmenstar1.digiDict.common.time.TimeDifferenceFormatter
import org.junit.BeforeClass
import org.junit.Test
import org.junit.runner.RunWith
import java.util.*
import kotlin.test.assertEquals

@RunWith(AndroidJUnit4::class)
class TimeDifferenceFormatterTests {
    private fun createFormatter(): TimeDifferenceFormatter {
        return TimeDifferenceFormatter(context)
    }

    private fun createDiffSeconds(weeks: Int = 0, days: Int = 0, hours: Int = 0, minutes: Int = 0): Long {
        return weeks * SECONDS_IN_WEEK + days * SECONDS_IN_DAY + hours * SECONDS_IN_HOUR + minutes * 60
    }

    @Test
    fun format_lessThanMinuteTest() {
        val formatter = createFormatter()
        val expectedStr = context.getString(R.string.less_than_minute)
        val actualStr = formatter.formatDifference(59)

        assertEquals(expectedStr, actualStr)
    }

    @Test
    fun formatTest() {
        fun testCase(diffSeconds: Long, expected: String) {
            val formatter = createFormatter()
            val actual = formatter.formatDifference(diffSeconds)

            assertEquals(expected, actual)
        }

        testCase(diffSeconds = createDiffSeconds(minutes = 5), expected = "5 minutes")
        testCase(diffSeconds = createDiffSeconds(minutes = 1), expected = "1 minute")
        testCase(diffSeconds = createDiffSeconds(hours = 5), expected = "5 hours")
        testCase(diffSeconds = createDiffSeconds(hours = 1), expected = "1 hour")
        testCase(diffSeconds = createDiffSeconds(days = 5), expected = "5 days")
        testCase(diffSeconds = createDiffSeconds(days = 1), expected = "1 day")
        testCase(diffSeconds = createDiffSeconds(weeks = 5), expected = "5 weeks")
        testCase(diffSeconds = createDiffSeconds(weeks = 1), expected = "1 week")

        testCase(diffSeconds = createDiffSeconds(hours = 5, minutes = 32), expected = "5 hours 32 minutes")
        testCase(
            diffSeconds = createDiffSeconds(weeks = 1, days = 2, hours = 3, minutes = 4),
            expected = "1 week 2 days 3 hours 4 minutes"
        )

        testCase(
            diffSeconds = createDiffSeconds(days = 2, hours = 3, minutes = 4),
            expected = "2 days 3 hours 4 minutes"
        )
    }

    companion object {
        private var context = InstrumentationRegistry.getInstrumentation().context

        @BeforeClass
        @JvmStatic
        fun beforeClass() {
            val conf = context.resources.configuration
            val newConf = Configuration(conf).apply {
                setLocale(Locale.forLanguageTag("en-US"))
            }

            context = context.createConfigurationContext(newConf)
        }
    }
}