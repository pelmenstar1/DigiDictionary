package io.github.pelmenstar1.digiDict.data

import android.os.Parcel
import android.os.Parcelable
import io.github.pelmenstar1.digiDict.common.android.readStringOrThrow
import io.github.pelmenstar1.digiDict.common.decimalDigitCount
import io.github.pelmenstar1.digiDict.common.equalsPattern
import io.github.pelmenstar1.digiDict.common.parsePositiveInt
import io.github.pelmenstar1.digiDict.common.unsafeNewArray

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

        override fun writeToParcel(dest: Parcel, flags: Int) {
            dest.writeString(text)
        }

        override fun equals(other: Any?) = equalsPattern(other) { o ->
            text == o.text
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

        val elements: Array<out String>

        constructor(parcel: Parcel) {
            val size = parcel.readInt()

            rawText = parcel.readStringOrThrow()
            elements = Array(size) { parcel.readStringOrThrow() }
        }

        constructor(elements: Array<out String>) {
            this.elements = elements
            rawText = createListRawText(elements)
        }

        internal constructor(elements: Array<out String>, rawText: String) {
            this.rawText = rawText
            this.elements = elements
        }

        override fun writeToParcel(dest: Parcel, flags: Int) {
            dest.writeInt(elements.size)
            dest.writeString(rawText)

            elements.forEach(dest::writeString)
        }

        override fun equals(other: Any?) = equalsPattern(other) { o ->
            return elements.contentEquals(o.elements)
        }

        override fun hashCode(): Int = elements.contentHashCode()

        override fun toString(): String {
            return "ComplexMeaning.List(elements=${elements.contentToString()})"
        }

        companion object CREATOR : Parcelable.Creator<List> {
            override fun createFromParcel(parcel: Parcel) = List(parcel)
            override fun newArray(size: Int) = arrayOfNulls<List>(size)
        }
    }

    abstract val type: MeaningType

    abstract override fun equals(other: Any?): Boolean
    abstract override fun hashCode(): Int
    abstract override fun toString(): String

    override fun describeContents() = 0

    companion object {
        const val COMMON_MARKER = 'C'
        const val LIST_MARKER = 'L'

        internal fun createCommonRawText(text: String): String {
            val buffer = CharArray(text.length + 1)
            buffer[0] = COMMON_MARKER
            text.toCharArray(buffer, 1)

            return String(buffer)
        }

        internal fun createListRawText(elements: Array<out String>): String {
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
                        append('\n')
                    }
                }
            }
        }

        fun throwInvalidFormat(rawText: String): Nothing {
            throw IllegalArgumentException("Invalid format (rawText=$rawText)")
        }

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

        fun parse(rawText: String): ComplexMeaning {
            if (rawText.isEmpty()) {
                throwInvalidFormat(rawText)
            }

            return when (rawText[0]) {
                COMMON_MARKER -> {
                    Common(rawText.substring(1), rawText)
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
                        var nextPos = rawText.indexOf('\n', prevPos)
                        if (nextPos < 0) {
                            nextPos = rawText.length
                        }

                        val element = rawText.substring(prevPos, nextPos)
                        elements[i] = element

                        prevPos = nextPos + 1
                    }

                    List(elements, rawText)
                }
                else -> throwInvalidFormat(rawText)
            }
        }

        inline fun iterateListElementRanges(
            rawText: String,
            block: (start: Int, end: Int) -> Unit
        ) {
            val length = rawText.length

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
                var nextDelimiterPos = rawText.indexOf('\n', i)
                if (nextDelimiterPos < 0) {
                    nextDelimiterPos = length
                }

                block(i, nextDelimiterPos)

                i = nextDelimiterPos + 1
            }
        }
    }
}