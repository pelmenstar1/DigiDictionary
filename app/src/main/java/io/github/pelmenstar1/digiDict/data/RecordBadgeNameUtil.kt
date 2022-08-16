package io.github.pelmenstar1.digiDict.data

import io.github.pelmenstar1.digiDict.common.EmptyArray
import io.github.pelmenstar1.digiDict.common.indexOf

/**
 * Provides util methods for encoding and decoding record badge name.
 *
 * An array of the names is encoded simply by joining using comma. If comma appears in the name, it should be preceded by backslash.
 * Then if comma isn't preceded by backslash, it's comma which splits elements of the array.
 */
object RecordBadgeNameUtil {
    /**
     * Encodes text to format acceptable to be record badge name. The only special character is comma, which replaced
     * to sequence `\,`
     */
    fun encode(text: String): String {
        if (text.isEmpty()) {
            throwEmptyBadgeName()
        }

        if (text.contains(',')) {
            // Give space for the found comma and another one that might appear
            return buildString(text.length + 4) { appendEncoded(text, this) }
        }

        return text
    }

    fun appendEncoded(text: String, sb: StringBuilder) {
        val textLength = text.length

        if (textLength == 0) {
            throwEmptyBadgeName()
        }

        var prevIndex = 0

        while (true) {
            val commaIndex = text.indexOf(',', prevIndex)
            if (commaIndex < 0) {
                break
            }

            sb.append(text, prevIndex, commaIndex)
            sb.append('\\')
            sb.append(',')

            prevIndex = commaIndex + 1
        }

        sb.append(text, prevIndex, textLength)
    }

    fun encodeArray(names: Array<out String>): String {
        val size = names.size
        when (size) {
            0 -> return ""
            1 -> return encode(names[0])
        }

        val lastIndex = size - 1
        val capacity = names.sumOf { it.length } +
                size * 4 + // for possible 2 commas.
                lastIndex // for delimiter commas without last element for which comma is unnecessary.

        val sb = StringBuilder(capacity)

        for (i in 0 until lastIndex) {
            // The method will do all needed checks.
            appendEncoded(names[i], sb)
            sb.append(',')
        }

        // There's always an unprocessed element because size > 1
        appendEncoded(names[lastIndex], sb)

        return sb.toString()
    }

    fun decode(text: String, start: Int = 0, end: Int = text.length): String {
        val length = end - start

        if (length == 0) {
            throwEmptyBadgeName()
        }

        var sb: StringBuilder? = null
        var prevIndex = start

        while (true) {
            val commaIndex = text.indexOf(',', prevIndex, end)

            if (commaIndex < 0) {
                break
            }

            if (commaIndex == 0 || text[commaIndex - 1] != '\\') {
                throw IllegalStateException("Text is expected to have only escaped commas")
            }

            if (sb == null) {
                sb = StringBuilder(length)
            }

            sb.append(text, prevIndex, commaIndex - 1)
            sb.append(',')

            prevIndex = commaIndex + 1
        }

        return if (sb != null) {
            sb.append(text, prevIndex, end)

            sb.toString()
        } else {
            text.substring(start, end)
        }
    }

    /**
     * Decodes raw form of an array of the badge names to array of strings.
     */
    fun decodeArray(text: String): Array<String> {
        val textLength = text.length

        if (textLength == 0) {
            return EmptyArray.STRING
        }

        val list = ArrayList<String>(4)
        var prevIndex = 0
        var commaSearchStartIndex = 0

        while (true) {
            val commaIndex = text.indexOf(',', commaSearchStartIndex)

            when {
                commaIndex < 0 -> break
                commaIndex == 0 -> throwEmptyBadgeName()
            }

            if (text[commaIndex - 1] == '\\') {
                commaSearchStartIndex = commaIndex + 1

                // Skip escaped commas.
                continue
            }

            val name = decode(text, prevIndex, commaIndex)

            prevIndex = commaIndex + 1
            commaSearchStartIndex = prevIndex

            list.addOrThrowIfContainsOrEmpty(name)
        }

        if (prevIndex < textLength) {
            val lastName = decode(text, prevIndex, textLength)

            list.addOrThrowIfContainsOrEmpty(lastName)
        }

        if (textLength > 1 && text[textLength - 2] != '\\' && text[textLength - 1] == ',') {
            // Last element is empty then. Example: "1,". But comma must be unescaped, because "1\," is valid.
            throwEmptyBadgeName()
        }

        return list.toArray(EmptyArray.STRING)
    }

    private fun throwEmptyBadgeName(): Nothing = throw IllegalStateException("Badge name is empty")
    private fun MutableList<String>.addOrThrowIfContainsOrEmpty(text: String) {
        if (text.isEmpty()) {
            throwEmptyBadgeName()
        }

        if (contains(text)) {
            throw IllegalStateException("Name duplicate")
        }

        add(text)
    }
}