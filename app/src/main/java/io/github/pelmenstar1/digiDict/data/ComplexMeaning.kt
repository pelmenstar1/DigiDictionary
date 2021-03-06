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
                    val otherText = other.text

                    if(text == otherText) {
                        this
                    } else {
                        List(ArraySet<String>(2).apply {
                            add(text)
                            add(otherText)
                        })
                    }
                }
                is List -> {
                    val otherElements = other.elements

                    if(otherElements.contains(text)) {
                        this
                    } else {
                        List(newArraySetFrom(otherElements, otherElements.size + 1).apply {
                            add(text)
                        })
                    }
                }
            }
        }

        override fun writeToParcel(dest: Parcel, flags: Int) {
            dest.writeString(text)
        }

        override fun equals(other: Any?): Boolean {
            if(other === this) return true
            if(other == null || other.javaClass != javaClass) return false

            other as Common

            return text == other.text
        }

        override fun hashCode(): Int = text.hashCode()

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

        val elements: Set<String>

        constructor(parcel: Parcel) {
            val size = parcel.readInt()
            validateListItemSize(size)

            _rawText = parcel.readStringOrThrow()

            elements = ArraySet<String>(size).apply {
                for(i in 0 until size) {
                    add(parcel.readStringOrThrow())
                }
            }
        }

        constructor(elements: Set<String>) {
            validateListItemSize(elements.size)

            _rawText = createListRawText(elements)
            this.elements = elements
        }

        constructor(elements: Array<out String>) {
            validateListItemSize(elements.size)

            this.elements = ArraySet<String>(elements.size).apply {
                elements.forEach(::add)
            }

            _rawText = createListRawText(this.elements)
        }

        @Suppress("UNCHECKED_CAST")
        constructor(firstElement: String, vararg elements: String) {
            // As there's already firstElement, elements size should be constrained to MAX_LIST_ITEM_SIZE - 1.
            // But if elements size is 0, this becomes -1, which is wrong as we have firstElement, so it should be at least 0 (which is valid value)
            validateListItemSize(max(0, elements.size - 1))

            this.elements = ArraySet<String>(elements.size + 1).apply {
                add(firstElement)
                elements.forEach(::add)
            }

            _rawText = createListRawText(this.elements)
        }

        constructor(elements: Set<String>, rawText: String) {
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
                        val newElements = newArraySetFrom(elements, elements.size + 1)
                        newElements.add(otherText)

                        List(newElements)
                    }
                }
                is List -> {
                    val otherElements = other.elements
                    val resultElements = newArraySetFrom(elements, elements.size + otherElements.size)

                    otherElements.forEach { resultElements.add(it) }

                    List(resultElements)
                }
            }
        }

        override fun writeToParcel(dest: Parcel, flags: Int) {
            dest.writeInt(elements.size)

            dest.writeString(rawText)

            elements.forEach(dest::writeString)
        }

        override fun equals(other: Any?): Boolean {
            if(other === this) return true
            if(other == null || javaClass != other.javaClass) return false

            other as List

            return elements == other.elements
        }

        override fun hashCode(): Int = elements.hashCode()

        override fun toString(): String {
            return "ComplexMeaning.List(elements=$elements)"
        }

        companion object CREATOR : Parcelable.Creator<List> {
            override fun createFromParcel(parcel: Parcel) = List(parcel)
            override fun newArray(size: Int) = arrayOfNulls<List>(size)
        }
    }

    abstract val type: MeaningType

    abstract fun mergedWith(other: ComplexMeaning): ComplexMeaning

    abstract override fun equals(other: Any?): Boolean
    abstract override fun hashCode(): Int
    abstract override fun toString(): String

    override fun describeContents() = 0

    companion object {
        /**
         * Max elements size (inclusive) of [List]
         */
        const val MAX_LIST_ITEM_SIZE = 100

        private fun newArraySetFrom(set: Set<String>, capacity: Int): ArraySet<String> {
            val newSet = ArraySet<String>(capacity)
            if(set is ArraySet<*>) {
                newSet.addAll(set)
            } else {
                set.forEach(newSet::add)
            }

            return newSet
        }

        private fun validateListItemSize(size: Int) {
            require(size in 0..MAX_LIST_ITEM_SIZE) { "Size of elements is negative or greater than $MAX_LIST_ITEM_SIZE" }
        }

        private fun createCommonRawText(text: String): String {
            val buffer = CharArray(text.length + 1)
            buffer[0] = 'C'
            text.getChars(buffer, 1)

            return String(buffer)
        }

        private fun createListRawText(elements: Set<String>): String {
            val size = elements.size
            if (size > 100) {
                throw IllegalArgumentException("values size is greater than 100")
            }

            val sizeDigitCount = size.decimalDigitCount()
            val contentStart = sizeDigitCount + 2

            var capacity = 2 /* L and @ */ + sizeDigitCount

            elements.forEachIndexed { index, element ->
                capacity += element.length

                // Increase capacity for new line symbol only if it's not the last element
                if(index < elements.size - 1) {
                    capacity++
                }
            }

            val buffer = CharArray(capacity)
            buffer[0] = 'L'
            buffer.write3DigitNumber(size, 1)
            buffer[sizeDigitCount + 1] = '@'

            var offset = contentStart

            elements.forEachIndexed { index, element ->
                element.getChars(buffer, offset)
                offset += element.length

                if (index < elements.size - 1) {
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
                    val elements = ArraySet<String>()

                    for(i in 0 until count) {
                        var nextDelimiterIndex = rawText.indexOf('\n', prevPos)

                        if (nextDelimiterIndex == -1) {
                            nextDelimiterIndex = rawText.length
                        }

                        val element = rawText.substring(prevPos, nextDelimiterIndex)

                        prevPos = nextDelimiterIndex + 1

                        elements.add(element)
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