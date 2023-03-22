package io.github.pelmenstar1.digiDict

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import io.github.pelmenstar1.digiDict.formatters.ResourcesRecordSearchPropertySetFormatter
import io.github.pelmenstar1.digiDict.search.RecordSearchProperty
import io.github.pelmenstar1.digiDict.search.RecordSearchPropertySet
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.test.assertEquals

@RunWith(AndroidJUnit4::class)
class ResourcesRecordSearchPropertySetFormatterTests {
    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private val allString: String
    private val expressionString: String
    private val meaningString: String

    init {
        with(context.resources) {
            allString = getString(R.string.home_searchProperty_all)
            expressionString = getString(R.string.expression)
            meaningString = getString(R.string.meaning)
        }
    }

    private fun createFormatter(): ResourcesRecordSearchPropertySetFormatter {
        return ResourcesRecordSearchPropertySetFormatter(context.resources)
    }

    private fun formatSpecificValueTestHelper(expectedStr: String, values: Array<RecordSearchProperty>) {
        val formatter = createFormatter()
        val set = RecordSearchPropertySet(values)

        assertEquals(expectedStr, formatter.format(set))

        // Test if cache works
        assertEquals(expectedStr, formatter.format(set))
    }

    @Test
    fun formatAllTest() {
        formatSpecificValueTestHelper(allString, RecordSearchProperty.values())
    }

    @Test
    fun formatExpressionTest() {
        formatSpecificValueTestHelper(expressionString, arrayOf(RecordSearchProperty.EXPRESSION))
    }

    @Test
    fun formatMeaningTest() {
        formatSpecificValueTestHelper(meaningString, arrayOf(RecordSearchProperty.MEANING))
    }

    @Test
    fun formatEmptyTest() {
        formatSpecificValueTestHelper("", emptyArray())
    }

    @Test
    fun formatSequenceTest() {
        val formatter = createFormatter()

        fun testCase(expectedStr: String, values: Array<RecordSearchProperty>) {
            val set = RecordSearchPropertySet(values)
            val actualStr = formatter.format(set)

            assertEquals(expectedStr, actualStr)
        }

        testCase(expressionString, arrayOf(RecordSearchProperty.EXPRESSION))
        testCase(meaningString, arrayOf(RecordSearchProperty.MEANING))
        testCase(allString, RecordSearchProperty.values())
        testCase(meaningString, arrayOf(RecordSearchProperty.MEANING))
        testCase(expressionString, arrayOf(RecordSearchProperty.EXPRESSION))
    }
}