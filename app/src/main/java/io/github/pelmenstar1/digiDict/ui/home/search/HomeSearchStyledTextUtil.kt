package io.github.pelmenstar1.digiDict.ui.home.search

import android.content.Context
import android.graphics.Typeface
import android.text.Spannable
import android.text.SpannableString
import android.text.SpannableStringBuilder
import android.text.style.StyleSpan
import android.util.Log
import io.github.pelmenstar1.digiDict.data.ComplexMeaning
import io.github.pelmenstar1.digiDict.search.RecordSearchMetadataProvider
import io.github.pelmenstar1.digiDict.ui.MeaningTextHelper

/**
 * Provides methods to created styled texts for search in home fragment.
 */
object HomeSearchStyledTextUtil {
    private const val TAG = "HomeSearchStyledTxtUtil"
    private const val FOUND_RANGE_STYLE = Typeface.BOLD

    /**
     * Returns a styled string (if neccessary) for expression [text] of a record.
     * If the expression has no special style, the method returns [String], otherwise [SpannableString].
     */
    fun createExpressionText(text: String, style: HomeSearchItemStyle): CharSequence {
        val foundRanges = style.foundRanges
        val rangeCount = foundRanges[0]

        // It's possible to have record found and expression having no found ranges.
        // For example, when a record is found by meaning. So, it's better to use plain text without
        // additional complications.
        if (rangeCount == 0) {
            return text
        }

        return SpannableString(text).also {
            setFoundRangesSpans(it, foundRanges, dataIndex = 1, textOffset = 0, rangeCount)
        }
    }

    /**
     * Creates a styled string (if neccessary) for [meaning] of a record.
     *
     * @param context a [Context] instance that is used to retrieve error string from resources if neccessary
     * @param meaning a meaning to create styled text for. The format should be as described in [ComplexMeaning]
     * @param style information about style of the meaning
     * @param forceThrow determines whether to throw an exception when one happens or return a string with error.
     * Basically used only for tests.
     *
     * @return a [String] instance if there's no special style to be applied, or [Spannable].
     */
    fun createMeaningText(
        context: Context,
        meaning: String,
        style: HomeSearchItemStyle,
        forceThrow: Boolean = false
    ): CharSequence {
        try {
            val foundRanges = style.foundRanges
            val startIndex = foundRanges[0] /* length of expression ranges */ * 2 + 1

            when (meaning[0]) {
                ComplexMeaning.COMMON_MARKER -> {
                    val rangeCount = foundRanges[startIndex]
                    val subMeaning = meaning.substring(1)

                    // It's possible to have record found and meaning having no found ranges.
                    // For example, when a record is found by expression. So, it's better to use plain text without
                    // additional complications.
                    if (rangeCount == 0) {
                        return subMeaning
                    }

                    return SpannableString(subMeaning).also {
                        setFoundRangesSpans(it, foundRanges, dataIndex = startIndex + 1, textOffset = 0, rangeCount)
                    }
                }
                ComplexMeaning.LIST_MARKER -> {
                    val formattedText = MeaningTextHelper.parseToFormatted(meaning)

                    // It's possible to have record found and meaning having no found ranges.
                    // For example, when a record is found by expression. So, it's better to use plain text without
                    // additional complications.
                    if (meaningDataHasNoRanges(foundRanges, startIndex)) {
                        return formattedText
                    }

                    val styledText = SpannableString(formattedText)

                    var dataIndex = startIndex

                    // Stores position in the formattedText where actual content of current
                    // meaning section starts.
                    //
                    // First two chars of the 'list' meaning is '• ', then the content starts.
                    var formattedTextIndex = 2

                    ComplexMeaning.iterateListElementRanges(meaning) { start, end ->
                        val rangeCount = foundRanges[dataIndex]
                        setFoundRangesSpans(
                            styledText,
                            foundRanges,
                            dataIndex + 1,
                            textOffset = formattedTextIndex,
                            rangeCount
                        )

                        dataIndex += rangeCount * 2 + 1

                        // After each meaning section 3 chars are inserted '\n• '.
                        // (end - start) gets length of the section
                        formattedTextIndex += (end - start) + 3
                    }

                    return styledText
                }
                else -> ComplexMeaning.throwInvalidFormat(meaning)
            }
        } catch (e: Exception) {
            Log.e(TAG, "while parsing meaning", e)

            if (forceThrow) {
                throw e
            }

            return MeaningTextHelper.getErrorMessageForFormatException(context, e)
        }
    }

    private fun meaningDataHasNoRanges(data: IntArray, startIndex: Int): Boolean {
        // If data, starting from startIndex, has at least one non-zero element, it means
        // that element is the length of the meaning's found ranges and then, the meaning has found ranges.
        for (i in startIndex until data.size) {
            if (data[i] != 0) {
                return false
            }
        }

        return true
    }

    /**
     * Sets spans, created by [createFoundRangeSpan], to specified [spannable].
     *
     * @param spannable a [Spannable] to set the spans to
     * @param foundRanges found ranges. An [IntArray] that has the format described in [RecordSearchMetadataProvider.calculateFoundRanges]
     * (starts and ends of ranges are sequential: start, end, start end and so).
     * @param dataIndex specifies from what index starting reading the ranges from [foundRanges].
     * @param textOffset specifies that should be applied to each range
     * @param rangeCount amount of ranges
     */
    private fun setFoundRangesSpans(
        spannable: Spannable,
        foundRanges: IntArray,
        dataIndex: Int,
        textOffset: Int,
        rangeCount: Int
    ) {
        val endIndex = dataIndex + rangeCount * 2
        var index = dataIndex

        while (index < endIndex) {
            val rangeStart = textOffset + foundRanges[index]
            val rangeEnd = textOffset + foundRanges[index + 1]

            spannable.setSpan(
                createFoundRangeSpan(),
                rangeStart,
                rangeEnd,
                SpannableStringBuilder.SPAN_INCLUSIVE_EXCLUSIVE
            )

            index += 2
        }
    }


    private fun createFoundRangeSpan(): StyleSpan {
        return StyleSpan(FOUND_RANGE_STYLE)
    }
}