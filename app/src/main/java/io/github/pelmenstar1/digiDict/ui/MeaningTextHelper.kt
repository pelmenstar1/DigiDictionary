package io.github.pelmenstar1.digiDict.ui

import android.content.Context
import android.util.Log
import io.github.pelmenstar1.digiDict.R
import io.github.pelmenstar1.digiDict.common.android.getLocaleCompat
import io.github.pelmenstar1.digiDict.common.parsePositiveInt
import io.github.pelmenstar1.digiDict.data.ComplexMeaning

object MeaningTextHelper {
    private const val TAG = "MeaningTextHelper"
    private const val BULLET_LIST_CHARACTER = '•'

    private fun throwIllegalFormat(meaning: String, cause: Exception? = null): Nothing {
        throw RuntimeException("Meaning has an illegal format ('$meaning')", cause)
    }

    fun format(meaning: String): String {
        return when (meaning[0]) {
            ComplexMeaning.COMMON_MARKER -> {
                // Leave single marker character.
                meaning.substring(1)
            }
            ComplexMeaning.LIST_MARKER -> {
                val textLength = meaning.length

                // Skip mark character
                val firstDelimiterIndex = meaning.indexOf('@', 1)
                if (firstDelimiterIndex < 0) {
                    throwIllegalFormat(meaning)
                }

                val count = meaning.parsePositiveInt(1, firstDelimiterIndex)
                if (count < 0) {
                    throwIllegalFormat(meaning)
                }

                var textIndex = firstDelimiterIndex + 1

                // Capacity of buffer consists of several parts:
                // - 2 * count is for '• ' sequence.
                // - textLength - textIndex is to leave list prefix part (L1@ or LN@ in general where N is length of elements)
                val buffer = CharArray(textLength - textIndex + 2 * count)
                var bufferIndex = 0

                while (textIndex < textLength) {
                    buffer[bufferIndex++] = BULLET_LIST_CHARACTER
                    buffer[bufferIndex++] = ' '

                    var nextDelimiterPos = meaning.indexOf('\n', textIndex)
                    if (nextDelimiterPos < 0) {
                        nextDelimiterPos = textLength
                    } else {
                        // To add leading new-line to buffer.
                        nextDelimiterPos++
                    }

                    // Write to buffer at its current position the part of text between previous and next element.
                    // Example: L2@ABCDABC\nL
                    //             ^      ^
                    //           from A  to new-line (inclusive)
                    meaning.toCharArray(buffer, bufferIndex, textIndex, nextDelimiterPos)

                    // Move bufferIndex forward to the end position of the written part to continue with a next element.
                    // Example: Meaning= L2@ABCDABC\nL
                    //          Buffer = '• ABCDABC\n'
                    //                      ^       ^
                    //                   previous | new position
                    //                   position | after a new-line
                    //                   (where A) \ - - - - - - - -
                    //                - - - - - - -/
                    bufferIndex += nextDelimiterPos - textIndex
                    textIndex = nextDelimiterPos
                }

                return String(buffer)
            }
            else -> throwIllegalFormat(meaning)
        }
    }

    fun formatOrErrorText(context: Context, meaning: String): String {
        return try {
            format(meaning)
        } catch (e: Exception) {
            Log.e(TAG, "", e)

            getErrorMessageForFormatException(context, e)
        }
    }

    fun getErrorMessageForFormatException(context: Context, cause: Exception): String {
        val message = cause.message ?: ""

        return try {
            val format = context.getString(R.string.meaningParseError)
            val locale = context.getLocaleCompat()

            String.format(locale, format, message)
        } catch (e: Exception) {
            Log.e(TAG, "failed to get an error message", e)

            "An error happened:\n$message"
        }
    }
}