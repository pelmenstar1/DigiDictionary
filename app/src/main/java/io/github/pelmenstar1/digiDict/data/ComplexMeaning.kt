package io.github.pelmenstar1.digiDict.data

import android.os.Parcel
import android.os.Parcelable
import androidx.collection.ArraySet
import io.github.pelmenstar1.digiDict.utils.*
import kotlin.math.max

enum class MeaningType {
    COMMON,
    LIST
}

sealed class ComplexMeaning : Parcelable {
    protected var _rawText: String = ""

    val rawText: String
        get() = _rawText

    /**
     * A meaning with only one entry.
     * Raw text scheme:
     * - Mark character is 'C'
     * - Actual text is after the mark character
     */
    class Common : ComplexMeaning {
        override val type: MeaningType
            get() = MeaningType.COMMON

        val text: String

        constructor(parcel: Parcel) {
            text = parcel.readStringOrThrow()
            _rawText = createCommonRawText(text)
        }

        constructor(text: String) {
            _rawText = createCommonRawText(text)
            this.text = text
        }

        constructor(text: String, rawText: String) {
            _rawText = rawText
            this.text = text
        }

        @Suppress("UNCHECKED_CAST")
        override fun mergedWith(other: ComplexMeaning): ComplexMeaning {
            return when(other) {
                is Common -> {
                    if(text == other.text) {
                        this
                    } else {
                        List(arrayOf(text, other.text))
                    }
                }
                is List -> {
                    val isListContainsText = other.elements.contains(text)

                    val otherElements = other.elements
                    val elements = arrayOfNulls<String>(otherElements.size + if(isListContainsText) 0 else 1)

                    System.arraycopy(otherElements, 0, elements, 0, otherElements.size)
                    if(!isListContainsText) {
                        elements[otherElements.size] = text
                    }

                    List(elements as Array<out String>)
                }
            }
        }

        override fun writeToParcel(dest: Parcel, flags: Int) {
            dest.writeString(text)
        }

        override fun toString(): String {
            return "ComplexMeaning.Common(text=$text)"
        }

        companion object CREATOR : Parcelable.Creator<Common> {
            override fun createFromParcel(parcel: Parcel) = Common(parcel)
            override fun newArray(size: Int) = arrayOfNulls<Common>(size)
        }
    }

    /**
     * A meaning with multiple entries.
     * Raw text scheme:
     * - Mark character is 'L'
     * - Count of elements is after the mark character
     * - Then as a delimiter '@' character
     * - Actual elements are after '@' divided by new line character.
     */
    class List : ComplexMeaning {
        override val type: MeaningType
            get() = MeaningType.LIST

        val elements: Array<out String>

        constructor(parcel: Parcel) {
            val size = parcel.readInt()

            validateListItemSize(size)

            _rawText = parcel.readStringOrThrow()
            elements = Array(size) { parcel.readStringOrThrow() }
        }

        constructor(elements: Array<out String>) {
            validateListItemSize(elements.size)

            _rawText = createListRawText(elements)
            this.elements = elements
        }

        @Suppress("UNCHECKED_CAST")
        constructor(firstElement: String, vararg elements: String) {
            // As there's already firstElement, elements size should be constrained to MAX_LIST_ITEM_SIZE - 1.
            // But if elements size is 0, this becomes -1, which is wrong as we have firstElement, so it should be at least 0 (which is valid value)
            validateListItemSize(max(0, elements.size - 1))

            val newElements = arrayOfNulls<String>(elements.size + 1) as Array<String>
            newElements[0] = firstElement
            System.arraycopy(elements, 0, newElements, 1, elements.size)

            this.elements = newElements
            _rawText = createListRawText(newElements)
        }

        constructor(elements: Array<out String>, rawText: String) {
           validateListItemSize(elements.size)

            _rawText = rawText
            this.elements = elements
        }

        @Suppress("UNCHECKED_CAST")
        override fun mergedWith(other: ComplexMeaning): ComplexMeaning {
            val elements = elements

            return when(other) {
                is Common -> {
                    val otherText = other.text

                    if(elements.contains(otherText)) {
                        this
                    } else {
                        val newElements = arrayOfNulls<String>(elements.size + 1)
                        System.arraycopy(elements, 0, newElements, 0, elements.size)
                        newElements[elements.size] = otherText

                        List(newElements as Array<out String>)
                    }
                }
                is List -> {
                    val otherElements = other.elements
                    val resultElements = ArraySet<String>(elements.size + otherElements.size)

                    elements.forEach { resultElements.add(it) }
                    otherElements.forEach { resultElements.add(it) }

                    List(resultElements.toTypedArray())
                }
            }
        }

        override fun writeToParcel(dest: Parcel, flags: Int) {
            dest.writeString(rawText)

            dest.writeInt(elements.size)
            elements.forEach { dest.writeString(it) }
        }

        override fun toString(): String {
            return "ComplexMeaning.List(elements=${elements.contentToString()})"
        }

        companion object CREATOR : Parcelable.Creator<List> {
            override fun createFromParcel(parcel: Parcel) = List(parcel)
            override fun newArray(size: Int) = arrayOfNulls<List>(size)
        }
    }

    abstract val type: MeaningType

    abstract fun mergedWith(other: ComplexMeaning): ComplexMeaning

    override fun equals(other: Any?): Boolean {
        if (other === this) return true
        if (other == null || other.javaClass != javaClass) return false

        other as ComplexMeaning

        return rawText == other.rawText
    }

    override fun hashCode(): Int {
        return rawText.hashCode()
    }

    override fun describeContents() = 0

    companion object {
        /**
         * Max elements size (inclusive) of [List]
         */
        const val MAX_LIST_ITEM_SIZE = 100

        private fun validateListItemSize(size: Int) {
            require(size in 0..MAX_LIST_ITEM_SIZE) { "Size of elements is negative or greater than $MAX_LIST_ITEM_SIZE" }
        }

        private fun createCommonRawText(text: String): String {
            val buffer = CharArray(text.length + 1)
            buffer[0] = 'C'
            text.getChars(buffer, 1)

            return String(buffer)
        }

        private fun createListRawText(elements: Array<out String>): String {
            val size = elements.size
            if (size > 100) {
                throw IllegalArgumentException("values size is greater than 100")
            }

            val sizeDigitCount = size.decimalDigitCount()
            val contentStart = sizeDigitCount + 2

            var capacity = 2 /* L and @ */ + sizeDigitCount

            for (i in elements.indices) {
                capacity += elements[i].length

                // Increase capacity for new line symbol only if it's not the last element
                if (i < elements.size - 1) {
                    capacity++
                }
            }

            val buffer = CharArray(capacity)
            buffer[0] = 'L'
            buffer.write3DigitNumber(size, 1)
            buffer[sizeDigitCount + 1] = '@'

            var offset = contentStart

            for (i in elements.indices) {
                val value = elements[i]

                value.getChars(buffer, offset)
                offset += value.length

                if (i < elements.size - 1) {
                    buffer[offset++] = '\n'
                }
            }

            return String(buffer)
        }

        private fun throwInvalidFormat(rawText: String): Nothing {
            throw IllegalArgumentException("Invalid format (rawText=$rawText)")
        }

        fun parse(rawText: String): ComplexMeaning {
            return when (rawText[0]) {
                'C' -> Common(rawText.substring(1), rawText)
                'L' -> {
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
                    val elements = Array(count) {
                        var nextDelimiterIndex = rawText.indexOf('\n', prevPos)

                        if (nextDelimiterIndex == -1) {
                            nextDelimiterIndex = rawText.length
                        }

                        val element = rawText.substring(prevPos, nextDelimiterIndex)

                        prevPos = nextDelimiterIndex + 1

                        element
                    }

                    List(elements, rawText)
                }
                else -> throwInvalidFormat(rawText)
            }
        }

        /**
         * Determines whether any element of meaning (specified by [rawText]) starts with particular [prefix].
         *
         * @param ignoreCase whether to ignore casing of characters.
         */
        fun anyElementStartsWith(
            rawText: String,
            prefix: String,
            ignoreCase: Boolean = false
        ): Boolean {
            return when (rawText[0]) {
                'C' -> {
                    rawText.startsWith(prefix, 1, ignoreCase)
                }
                'L' -> {
                    // Skip mark character
                    val firstDelimiterIndex = rawText.indexOf('@', 1)
                    if (firstDelimiterIndex < 0) {
                        throwInvalidFormat(rawText)
                    }

                    var i = firstDelimiterIndex + 1
                    while (true) {
                        if (rawText.startsWith(prefix, i, ignoreCase)) {
                            return true
                        }

                        val nextDelimiterPos = rawText.indexOf('\n', i)
                        if (nextDelimiterPos == -1) {
                            break
                        }

                        i = nextDelimiterPos + 1
                    }

                    false
                }
                else -> throwInvalidFormat(rawText)
            }
        }
    }
}