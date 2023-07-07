package io.github.pelmenstar1.digiDict.data

import android.os.Parcel
import android.os.Parcelable
import io.github.pelmenstar1.digiDict.common.android.readStringOrThrow
import io.github.pelmenstar1.digiDict.common.decimalDigitCount
import io.github.pelmenstar1.digiDict.common.equalsPattern
import io.github.pelmenstar1.digiDict.common.parsePositiveInt
import io.github.pelmenstar1.digiDict.common.unsafeNewArray


class ComplexMeaning private constructor(
    val rawText: String,
    // elements expected to be non-null when it's a list type meaning
    private val elements: Array<out String>?
) : Parcelable {
    val elementCount: Int
        get() = elements?.size ?: 1

    fun getElement(index: Int): String {
        return if (elements != null) {
            if (index !in elements.indices) throw IndexOutOfBoundsException("index")

            elements[index]
        } else {
            if (index != 0) throw IndexOutOfBoundsException("index")

            rawText.substring(1)
        }
    }

    override fun writeToParcel(dest: Parcel, flags: Int) {
        dest.writeString(rawText)
    }

    override fun describeContents() = 0

    override fun equals(other: Any?): Boolean {
        return equalsPattern(other) { o ->
            rawText == o.rawText
        }
    }

    override fun hashCode(): Int {
        return rawText.hashCode()
    }

    override fun toString(): String {
        return if (elements != null) {
            "ComplexMeaning(type=LIST, elements=${elements.contentToString()})"
        } else {
            "ComplexMeaning(type=COMMON, text=${getElement(0)})"
        }
    }

    companion object {
        const val COMMON_MARKER = 'C'
        const val LIST_MARKER = 'L'

        const val LIST_OLD_ELEMENT_SEPARATOR = '\n'

        // 0x1D - group separator.
        const val LIST_NEW_ELEMENT_SEPARATOR = 0x1D.toChar()

        @JvmField
        val CREATOR = object : Parcelable.Creator<ComplexMeaning> {
            override fun createFromParcel(source: Parcel): ComplexMeaning {
                val rawText = source.readStringOrThrow()

                return parse(rawText)
            }

            override fun newArray(size: Int) = arrayOfNulls<ComplexMeaning>(size)
        }

        fun common(text: String): ComplexMeaning {
            return ComplexMeaning(createCommonRawText(text), elements = null)
        }

        fun list(elements: Array<out String>): ComplexMeaning {
            return ComplexMeaning(createListRawText(elements), elements)
        }

        private fun createCommonRawText(text: String): String {
            val buffer = CharArray(text.length + 1)
            buffer[0] = COMMON_MARKER
            text.toCharArray(buffer, 1)

            return String(buffer)
        }

        private fun createListRawText(elements: Array<out String>): String {
            val size = elements.size
            val lastIndex = size - 1

            var capacity = 2 /* L and @ */ + size.decimalDigitCount()

            elements.forEachIndexed { index, element ->
                capacity += element.length

                // Increase capacity for new line symbol only if it's not the last element
                if (index < lastIndex) {
                    capacity++
                }
            }

            return buildString(capacity) {
                append(LIST_MARKER)
                append(size)
                append('@')

                elements.forEachIndexed { index, element ->
                    append(element)

                    if (index < lastIndex) {
                        append(LIST_NEW_ELEMENT_SEPARATOR)
                    }
                }
            }
        }

        fun throwInvalidFormat(rawText: String): Nothing {
            throw IllegalArgumentException("Invalid format (rawText=$rawText)")
        }

        /**
         * Returns how much distinct meaning is stored in a raw meaning string.
         * The method does a little bit of validating the meaning but it may return wrong value in case the meaning is
         * encoded incorrectly.
         *
         * @throws IllegalArgumentException if rawText is in invalid format.
         */
        fun getMeaningCount(rawText: String): Int {
            if (rawText.isEmpty()) {
                throwInvalidFormat(rawText)
            }

            return when (rawText[0]) {
                COMMON_MARKER -> 1
                LIST_MARKER -> {
                    // Skip mark character
                    val firstDelimiterIndex = rawText.indexOf('@', 1)
                    if (firstDelimiterIndex < 0) {
                        throwInvalidFormat(rawText)
                    }

                    // parsePositiveInt() returns -1 when format is invalid.
                    val count = rawText.parsePositiveInt(1, firstDelimiterIndex)
                    if (count < 0) {
                        throwInvalidFormat(rawText)
                    }

                    count
                }
                else -> throwInvalidFormat(rawText)
            }
        }

        /**
         * Parses raw meaning string to the object-like form that always contains only valid meaning.
         *
         * @throws IllegalArgumentException if the [rawText] is in invalid format.
         */
        fun parse(rawText: String): ComplexMeaning {
            if (rawText.isEmpty()) {
                throwInvalidFormat(rawText)
            }

            return when (rawText[0]) {
                COMMON_MARKER -> {
                    ComplexMeaning(rawText, elements = null)
                }
                LIST_MARKER -> {
                    // Skip mark character
                    val firstDelimiterIndex = rawText.indexOf('@', 1)
                    if (firstDelimiterIndex < 0) {
                        throwInvalidFormat(rawText)
                    }

                    // parsePositiveInt() returns -1 when format is invalid.
                    val count = rawText.parsePositiveInt(1, firstDelimiterIndex)
                    if (count < 0) {
                        throwInvalidFormat(rawText)
                    }

                    // Content starts after '@' symbol, so +1
                    val contentStart = firstDelimiterIndex + 1

                    var prevPos = contentStart
                    val elements = unsafeNewArray<String>(count)

                    for (i in 0 until count) {
                        val nextPos = indexOfListSeparatorOrLength(rawText, prevPos)

                        val element = rawText.substring(prevPos, nextPos)
                        elements[i] = element

                        prevPos = nextPos + 1
                    }

                    ComplexMeaning(rawText, elements)
                }
                else -> throwInvalidFormat(rawText)
            }
        }

        /**
         * Iterates each list element range of given list meaning.
         *
         * @param rawText a raw list meaning string to iterate the ranges of
         * @param block a lambda that processes each range. Start is inclusive, end is exclusive.
         */
        inline fun iterateListElementRanges(
            rawText: String,
            block: (start: Int, end: Int) -> Unit
        ) {
            // Skip mark character
            val firstDelimiterIndex = rawText.indexOf('@', 1)
            if (firstDelimiterIndex < 0) {
                throwInvalidFormat(rawText)
            }

            val count = rawText.parsePositiveInt(1, firstDelimiterIndex)
            when {
                count < 0 -> throwInvalidFormat(rawText)
                count == 0 -> return
            }

            var i = firstDelimiterIndex + 1

            repeat(count) {
                val nextDelimiterPos = indexOfListSeparatorOrLength(rawText, i)

                block(i, nextDelimiterPos)

                i = nextDelimiterPos + 1
            }
        }

        /**
         * Converts a list meaning encoded in old format (with \n as element separator) to the new list meaning format.
         */
        fun recodeListOldFormatToNew(meaning: String): String {
            return meaning.replace(LIST_OLD_ELEMENT_SEPARATOR, LIST_NEW_ELEMENT_SEPARATOR)
        }

        /**
         * Converts a list meaning encoded in new format to the old meaning format.
         *
         * [meaning] should not contain any \n as a part of the list elements.
         */
        fun recodeListNewFormatToOld(meaning: String): String {
            return meaning.replace(LIST_NEW_ELEMENT_SEPARATOR, LIST_OLD_ELEMENT_SEPARATOR)
        }

        /**
         * Finds an index of [ComplexMeaning.LIST_NEW_ELEMENT_SEPARATOR] in the given string starting from [startIndex].
         * If the index is not found, returns length of the string.
         */
        fun indexOfListSeparatorOrLength(str: String, startIndex: Int): Int {
            var index = str.indexOf(LIST_NEW_ELEMENT_SEPARATOR, startIndex)
            if (index < 0) {
                index = str.length
            }

            return index
        }

        /**
         * Determines whether raw meaning string is a correctly encoded meaning.
         */
        fun isValid(rawText: String): Boolean {
            val textLength = rawText.length

            if (textLength < 2) {
                return false
            }

            return when (rawText[0] /* mark char */) {
                COMMON_MARKER -> true
                LIST_MARKER -> {
                    // Skip mark character
                    val firstDelimiterIndex = rawText.indexOf('@', 1)
                    if (firstDelimiterIndex < 0) {
                        return false
                    }

                    val count = rawText.parsePositiveInt(1, firstDelimiterIndex)
                    if (count <= 0) {
                        return false
                    }

                    // There's at least one element without any separators
                    var actualElementCount = 1

                    for (i in firstDelimiterIndex until textLength) {
                        if (rawText[i] == LIST_NEW_ELEMENT_SEPARATOR) {
                            actualElementCount++
                        }
                    }

                    actualElementCount == count
                }
                else -> false
            }
        }
    }
}