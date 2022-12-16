package io.github.pelmenstar1.digiDict.ui

import android.content.Context
import android.util.Log
import io.github.pelmenstar1.digiDict.R
import io.github.pelmenstar1.digiDict.common.getLocaleCompat
import io.github.pelmenstar1.digiDict.common.parsePositiveInt

object MeaningTextHelper {
    class FormatException(message: String, cause: Throwable?) : Exception(message, cause)

    private const val TAG = "MeaningTextHelper"

    private fun throwIllegalFormat(meaning: String, cause: Exception? = null): Nothing {
        throw FormatException("Meaning has an illegal format ('$meaning')", cause)
    }

    fun parseToFormatted(meaning: String): String {
        try {
            return when (meaning[0]) {
                'C' -> {
                    // Leave single marker character.
                    meaning.substring(1)
                }
                'L' -> {
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
                        buffer[bufferIndex++] = '•'
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
        } catch (e: Exception) {
            if (e is FormatException) {
                throw e
            } else {
                throwIllegalFormat(meaning, e)
            }
        }
    }

    fun parseToFormattedAndHandleErrors(context: Context, meaning: String): String {
        return try {
            parseToFormatted(meaning)
        } catch (e: Exception) {
            Log.e(TAG, "", e)

            getErrorMessageForException(context, e)
        }
    }

    private fun getErrorMessageForException(context: Context, e: Exception): String {
        return try {
            val format = context.getString(R.string.meaningParseError)
            val locale = context.getLocaleCompat()

            String.format(locale, format, e.message ?: "")
        } catch (e: Exception) {
            Log.e(TAG, "failed to get an error message", e)

            "An error happened: ${e.message ?: ""}"
        }
    }
}