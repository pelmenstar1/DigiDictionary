package io.github.pelmenstar1.digiDict

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import io.github.pelmenstar1.digiDict.formatters.ResourcesRecordSearchPropertySetFormatter
import io.github.pelmenstar1.digiDict.search.RecordSearchProperty
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
        assertEquals(expectedStr, formatter.format(values))

        // Test if cache works
        assertEquals(expectedStr, formatter.format(values))
    }

    @Test
    fun formatAllTest() {
        formatSpecificValueTestHelper(allString, RecordSearchProperty.values())

        // In case the contract of having no duplicate is not met, the formatter is expected to work anyway
        formatSpecificValueTestHelper(
            allString,
            arrayOf(
                RecordSearchProperty.EXPRESSION,
                RecordSearchProperty.MEANING,
                RecordSearchProperty.MEANING,
                RecordSearchProperty.EXPRESSION
            )
        )
    }

    @Test
    fun formatExpressionTest() {
        formatSpecificValueTestHelper(expressionString, arrayOf(RecordSearchProperty.EXPRESSION))

        // In case the contract of having no duplicate is not met, the formatter is expected to work anyway
        formatSpecificValueTestHelper(
            expressionString,
            arrayOf(RecordSearchProperty.EXPRESSION, RecordSearchProperty.EXPRESSION)
        )
    }

    @Test
    fun formatMeaningTest() {
        formatSpecificValueTestHelper(meaningString, arrayOf(RecordSearchProperty.MEANING))

        // In case the contract of having no duplicate is not met, the formatter is expected to work anyway
        formatSpecificValueTestHelper(
            meaningString,
            arrayOf(RecordSearchProperty.MEANING, RecordSearchProperty.MEANING)
        )
    }

    @Test
    fun formatEmptyTest() {
        assertEquals("", createFormatter().format(emptyArray()))
    }

    @Test
    fun formatSequenceTest() {
        val formatter = createFormatter()

        assertEquals(expressionString, formatter.format(arrayOf(RecordSearchProperty.EXPRESSION)))
        assertEquals(meaningString, formatter.format(arrayOf(RecordSearchProperty.MEANING)))
        assertEquals(allString, formatter.format(RecordSearchProperty.values()))
        assertEquals(meaningString, formatter.format(arrayOf(RecordSearchProperty.MEANING)))
        assertEquals(expressionString, formatter.format(arrayOf(RecordSearchProperty.EXPRESSION)))
    }
}