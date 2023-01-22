package io.github.pelmenstar1.digiDict.ui.home.search

import android.content.Context
import android.graphics.Typeface
import android.text.Spannable
import android.text.SpannableString
import android.text.SpannableStringBuilder
import android.text.style.StyleSpan
import android.util.Log
import io.github.pelmenstar1.digiDict.data.ComplexMeaning
import io.github.pelmenstar1.digiDict.ui.MeaningTextHelper

/**
 * Provides methods to created styled texts for search in home fragment.
 */
object HomeSearchStyledTextUtil {
    private const val TAG = "HomeSearchStyledTxtUtil"
    const val FOUND_RANGE_STYLE = Typeface.BOLD

    /**
     * Returns a styled string (if possible) for expression [text] of a record.
     * If the expression has no special style, the method returns [String], otherwise [SpannableString].
     */
    fun createExpressionText(text: String, metadataProvider: HomeSearchMetadataProvider): CharSequence {
        val data = metadataProvider.calculateFoundRangesInExpression(text)
        val rangeCount = data[0]

        // It's possible to have record found and expression having no found ranges.
        // For example, when a record is found by meaning. So, it's better to use plain text without
        // additional complications.
        if (rangeCount == 0) {
            return text
        }

        return SpannableString(text).also { builder ->
            setSpans(builder, data, dataIndex = 1, textOffset = 0, rangeCount)
        }
    }

    /**
     * Returns a styled string (if possible) for [meaning] of a record.
     * The format of a meaning should be as described in [ComplexMeaning].
     *
     * If the [meaning] has no special style, the method returns [String], otherwise [Spannable].
     */
    fun createMeaningText(
        context: Context,
        meaning: String,
        metadataProvider: HomeSearchMetadataProvider
    ): CharSequence {
        try {
            val data = metadataProvider.calculateFoundRangesInMeaning(meaning)

            when (meaning[0]) {
                ComplexMeaning.COMMON_MARKER -> {
                    val rangeCount = data[0]
                    val subMeaning = meaning.substring(1)

                    // It's possible to have record found and meaning having no found ranges.
                    // For example, when a record is found by expression. So, it's better to use plain text without
                    // additional complications.
                    if (rangeCount == 0) {
                        return subMeaning
                    }

                    return SpannableString(subMeaning).also { builder ->
                        setSpans(builder, data, dataIndex = 1, textOffset = 0, rangeCount)
                    }
                }
                ComplexMeaning.LIST_MARKER -> {
                    // It's faster to check whether data has any ranges than
                    // to create styled text through SpannableStringBuilder, that is really slow.
                    if (meaningDataHasNoRanges(data)) {
                        return MeaningTextHelper.parseToFormattedAndHandleErrors(context, meaning)
                    }

                    val builder = SpannableStringBuilder()
                    var isFirstElement = true

                    var dataIndex = 0

                    ComplexMeaning.iterateListElementRanges(meaning) { start, end ->
                        val prefix = if (isFirstElement) {
                            "${MeaningTextHelper.BULLET_LIST_CHARACTER} "
                        } else {
                            "\n${MeaningTextHelper.BULLET_LIST_CHARACTER} "
                        }

                        isFirstElement = false

                        builder.append(prefix)

                        val meaningPartStart = builder.length
                        builder.append(meaning, start, end)

                        val rangeCount = data[dataIndex]
                        setSpans(builder, data, dataIndex + 1, textOffset = meaningPartStart, rangeCount)
                        dataIndex += rangeCount * 2 + 1
                    }

                    return builder
                }
                else -> ComplexMeaning.throwInvalidFormat(meaning)
            }
        } catch (e: Exception) {
            Log.e(TAG, "while parsing meaning", e)

            return MeaningTextHelper.getErrorMessageForFormatException(context, e)
        }
    }

    private fun meaningDataHasNoRanges(data: IntArray): Boolean {
        var index = 0

        while (index < data.size) {
            val rangeCount = data[index]
            if (rangeCount > 0) {
                return false
            }

            index++
        }

        return true
    }

    /**
     * Sets spans, created by [createStyleSpan], to specified [spannable].
     *
     * @param spannable a [Spannable] to set the spans to
     * @param data an [IntArray] that has the format described in [HomeSearchMetadataProvider]
     * (starts and ends of ranges are sequential: start, end, start end and so).
     * @param dataIndex specifies from what index starting reading the ranges from [data].
     * @param textOffset specifies that should be applied to each range
     * @param rangeCount amount of ranges
     */
    private fun setSpans(
        spannable: Spannable,
        data: IntArray,
        dataIndex: Int,
        textOffset: Int,
        rangeCount: Int
    ) {
        val endIndex = dataIndex + rangeCount * 2
        var index = dataIndex

        while (index < endIndex) {
            val rangeStart = textOffset + data[index]
            val rangeEnd = textOffset + data[index + 1]

            spannable.setSpan(createStyleSpan(), rangeStart, rangeEnd, SpannableStringBuilder.SPAN_INCLUSIVE_EXCLUSIVE)

            index += 2
        }
    }


    private fun createStyleSpan(): StyleSpan {
        return StyleSpan(FOUND_RANGE_STYLE)
    }
}