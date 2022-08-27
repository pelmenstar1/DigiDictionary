package io.github.pelmenstar1.digiDict.ui

import io.github.pelmenstar1.digiDict.common.parsePositiveInt

object MeaningTextHelper {
    private fun throwIllegalFormat(text: String): Nothing {
        throw IllegalArgumentException("Meaning has illegal format ($text)")
    }

    fun parseToFormatted(text: String): String {
        return when (text[0]) {
            'C' -> {
                // Leave single marker character.
                text.substring(1)
            }
            'L' -> {
                val textLength = text.length

                // Skip mark character
                val firstDelimiterIndex = text.indexOf('@', 1)
                if (firstDelimiterIndex < 0) {
                    throwIllegalFormat(text)
                }

                val count = text.parsePositiveInt(1, firstDelimiterIndex)
                if (count < 0) {
                    throwIllegalFormat(text)
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

                    var nextDelimiterPos = text.indexOf('\n', textIndex)
                    if (nextDelimiterPos < 0) {
                        nextDelimiterPos = textLength
                    } else {
                        // To add leading new-line to buffer.
                        nextDelimiterPos++
                    }

                    // Write to buffer at its current position the part of text between previous and next element.
                    // Example: L2@ABCDABC\nL
                    //            ^       ^
                    //          from @   to new-line
                    text.toCharArray(buffer, bufferIndex, textIndex, nextDelimiterPos)

                    // Move bufferIndex forward to the end position of the written part to continue with a next element.
                    // Example: Meaning= L2@ABCDABC\nL
                    //          Buffer = '• ABCDABC\n'
                    //                     ^         ^
                    //                   previous | new position
                    //                   position | after new-line
                    //                (where space)\ - - - - - - - -
                    //                - - - - - - - /
                    bufferIndex += nextDelimiterPos - textIndex
                    textIndex = nextDelimiterPos
                }

                return String(buffer)
            }
            else -> throwIllegalFormat(text)
        }
    }
}