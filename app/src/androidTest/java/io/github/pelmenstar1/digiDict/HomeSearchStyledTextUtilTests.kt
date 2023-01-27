package io.github.pelmenstar1.digiDict

import android.text.Spanned
import android.text.style.StyleSpan
import androidx.core.text.getSpans
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import io.github.pelmenstar1.digiDict.common.mapToArray
import io.github.pelmenstar1.digiDict.ui.MeaningTextHelper
import io.github.pelmenstar1.digiDict.ui.home.search.HomeSearchItemStyle
import io.github.pelmenstar1.digiDict.ui.home.search.HomeSearchStyledTextUtil
import io.github.pelmenstar1.digiDict.utils.IntRangeSection
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertIs

@RunWith(AndroidJUnit4::class)
class HomeSearchStyledTextUtilTests {
    @Test
    fun createExpressionTextTest() {
        fun testCase(expr: String, foundRanges: Array<IntRange>) {
            val foundRangesIntArray = IntArray(foundRanges.size * 2 + 1).also { buffer ->
                buffer[0] = foundRanges.size
                var index = 1

                for (range in foundRanges) {
                    buffer[index++] = range.first

                    // end should be exclusive where-as IntRange's end is inclusive
                    buffer[index++] = range.last + 1
                }
            }

            val style = HomeSearchItemStyle(foundRangesIntArray)
            val actualStyledText = HomeSearchStyledTextUtil.createExpressionText(expr, style)

            if (foundRanges.isEmpty()) {
                assertIs<String>(actualStyledText)
            } else {
                assertIs<Spanned>(actualStyledText)

                val actualString = actualStyledText.toString()
                assertEquals(expr, actualString)

                val actualRangesRaw = actualStyledText.getSpans<StyleSpan>()
                val actualRanges = actualRangesRaw.mapToArray { tag ->
                    val start = actualStyledText.getSpanStart(tag)
                    val end = actualStyledText.getSpanEnd(tag)

                    start until end
                }

                assertContentEquals(foundRanges, actualRanges)
            }
        }

        testCase(expr = "abc", foundRanges = emptyArray())
        testCase(expr = "abc", foundRanges = arrayOf(0 until 1))
        testCase(expr = "abc", foundRanges = arrayOf(0 until 3))
        testCase(expr = "a", foundRanges = arrayOf(0 until 1))
        testCase(expr = "abc", foundRanges = arrayOf(0 until 1, 2 until 3))
    }

    @Test
    fun createMeaningTextTest() {
        fun testCase(meaning: String, foundSections: Array<IntRangeSection>, expectedRanges: Array<IntRange>) {
            val context = InstrumentationRegistry.getInstrumentation().context

            val foundRangesIntArray = IntArray(foundSections.sumOf { it.ranges.size * 2 + 1 } + 1).also { buffer ->
                buffer[0] = 0 // length of expression ranges. there's none actually, for tests
                var index = 1
                for (section in foundSections) {
                    buffer[index++] = section.ranges.size

                    for (range in section.ranges) {
                        buffer[index++] = range.first

                        // end should be exclusive where-as IntRange's end is inclusive
                        buffer[index++] = range.last + 1
                    }
                }
            }

            val style = HomeSearchItemStyle(foundRangesIntArray)
            val actualStyledText = HomeSearchStyledTextUtil.createMeaningText(context, meaning, style)
            if (foundSections.isEmpty()) {
                assertIs<String>(actualStyledText)
            } else {
                assertIs<Spanned>(actualStyledText)

                val actualString = actualStyledText.toString()
                val expectedString = MeaningTextHelper.parseToFormatted(meaning)
                assertEquals(expectedString, actualString)

                val actualRangesRaw = actualStyledText.getSpans<StyleSpan>()

                val actualRanges = actualRangesRaw.mapToArray { tag ->
                    val start = actualStyledText.getSpanStart(tag)
                    val end = actualStyledText.getSpanEnd(tag)

                    start until end
                }

                assertContentEquals(expectedRanges, actualRanges)
            }
        }

        testCase(
            meaning = "CABC",
            foundSections = emptyArray(),
            expectedRanges = emptyArray()
        )

        testCase(
            meaning = "CABC",
            foundSections = arrayOf(IntRangeSection(0 until 1)),
            expectedRanges = arrayOf(0 until 1)
        )

        testCase(
            meaning = "CABC",
            foundSections = arrayOf(IntRangeSection(0 until 3)),
            expectedRanges = arrayOf(0 until 3)
        )

        testCase(
            meaning = "CABCD",
            foundSections = arrayOf(IntRangeSection(0 until 2, 3 until 4)),
            expectedRanges = arrayOf(0 until 2, 3 until 4)
        )

        testCase(
            meaning = "CA",
            foundSections = arrayOf(IntRangeSection(0 until 1)),
            expectedRanges = arrayOf(0 until 1)
        )

        testCase(
            meaning = "L2@ABCD\nFFF",
            foundSections = arrayOf(IntRangeSection(1 until 3), IntRangeSection(0 until 2)),
            expectedRanges = arrayOf(3 until 5, 9 until 11)
        )

        testCase(
            meaning = "L2@ABCD\nFFF",
            foundSections = emptyArray(),
            expectedRanges = emptyArray()
        )


    }
}