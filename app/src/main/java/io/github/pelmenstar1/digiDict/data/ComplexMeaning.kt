package io.github.pelmenstar1.digiDict.data

import android.os.Parcel
import android.os.Parcelable
import androidx.collection.ArraySet
import io.github.pelmenstar1.digiDict.utils.*

enum class MeaningType {
    COMMON,
    LIST
}

sealed class ComplexMeaning : Parcelable {
    var rawText: String = ""
        protected set

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
            rawText = createCommonRawText(text)
        }

        constructor(text: String) {
            this.text = text
            rawText = createCommonRawText(text)
        }

        constructor(text: String, rawText: String) {
            this.text = text
            this.rawText = rawText
        }

        @Suppress("UNCHECKED_CAST")
        override fun mergedWith(other: ComplexMeaning): ComplexMeaning {
            return when (other) {
                is Common -> {
                    val otherText = other.text

                    if (text == otherText) {
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

                    if (otherElements.contains(text)) {
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
            if (other === this) return true
            if (other == null || other.javaClass != javaClass) return false

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

            rawText = parcel.readStringOrThrow()

            elements = ArraySet<String>(size).apply {
                for (i in 0 until size) {
                    add(parcel.readStringOrThrow())
                }
            }
        }

        constructor(elements: Set<String>) {
            this.elements = elements
            rawText = createListRawText(elements)
        }

        constructor(elements: Array<out String>) {
            val set = ArraySet<String>(elements.size).apply {
                addAllArray(elements)
            }

            this.elements = set

            rawText = createListRawText(set)
        }

        @Suppress("UNCHECKED_CAST")
        constructor(firstElement: String, vararg elements: String) {
            val set = ArraySet<String>(elements.size + 1).apply {
                add(firstElement)
                addAllArray(elements)
            }

            this.elements = set

            rawText = createListRawText(set)
        }

        constructor(elements: Set<String>, rawText: String) {
            this.rawText = rawText
            this.elements = elements
        }

        @Suppress("UNCHECKED_CAST")
        override fun mergedWith(other: ComplexMeaning): ComplexMeaning {
            val elements = elements

            return when (other) {
                is Common -> {
                    val otherText = other.text

                    if (elements.contains(otherText)) {
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
                    resultElements.addAllSet(otherElements)

                    List(resultElements)
                }
            }
        }

        override fun writeToParcel(dest: Parcel, flags: Int) {
            dest.writeInt(elements.size)
            dest.writeString(rawText)

            elements.forEachFast(dest::writeString)
        }

        override fun equals(other: Any?): Boolean {
            if (other === this) return true
            if (other == null || javaClass != other.javaClass) return false

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
        internal fun createCommonRawText(text: String): String {
            val buffer = CharArray(text.length + 1)
            buffer[0] = 'C'
            text.toCharArray(buffer, 1)

            return String(buffer)
        }

        internal fun createListRawText(elements: Set<String>): String {
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
                append('L')
                append(size)
                append('@')

                elements.forEachIndexed { index, element ->
                    append(element)

                    if (index < lastIndex) {
                        append('\n')
                    }
                }
            }
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

                    for (i in 0 until count) {
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