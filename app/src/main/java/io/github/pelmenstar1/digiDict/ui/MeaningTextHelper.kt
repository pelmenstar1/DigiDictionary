package io.github.pelmenstar1.digiDict.ui

import io.github.pelmenstar1.digiDict.data.ComplexMeaning
import io.github.pelmenstar1.digiDict.utils.parsePositiveInt

object MeaningTextHelper {
    private fun throwIllegalFormat(rawText: String): Nothing {
        throw IllegalArgumentException("Raw text has illegal format ($rawText)")
    }

    @JvmStatic // for view binding
    fun parseRawMeaningToFormatted(rawText: String): String {
        return when(rawText[0]) {
            'C' -> {
                rawText.substring(1)
            }
            'L' -> {
                // Skip mark character
                val firstDelimiterIndex = rawText.indexOf('@', 1)
                val count = rawText.parsePositiveInt(1, firstDelimiterIndex)

                if(count !in 0..ComplexMeaning.MAX_LIST_ITEM_SIZE) {
                    throwIllegalFormat(rawText)
                }

                var offset = firstDelimiterIndex + 1

                // 2 characters are added for each item.
                buildString(capacity = rawText.length + count * 2) {
                    while(offset < rawText.length) {
                        val nextDelimiterPos = rawText.indexOf('\n', offset)

                        append('•')
                        append(' ')
                        if (nextDelimiterPos == -1) {
                            append(rawText, offset, rawText.length)
                            break
                        } else {
                            append(rawText, offset, nextDelimiterPos + 1)
                            offset = nextDelimiterPos + 1
                        }
                    }
                }
            }
            else -> throwIllegalFormat(rawText)
        }
    }
}