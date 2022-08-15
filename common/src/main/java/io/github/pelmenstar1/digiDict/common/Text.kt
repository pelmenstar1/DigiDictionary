@file:Suppress("NOTHING_TO_INLINE")

package io.github.pelmenstar1.digiDict.common

import android.text.GetChars

const val NULL_CHAR = 0.toChar()

fun Int.decimalDigitCount(): Int {
    return when {
        this < 10 -> 1
        this < 100 -> 2
        this < 1000 -> 3
        this < 10000 -> 4
        this < 100000 -> 5
        this < 1000000 -> 6
        this < 10000000 -> 7
        this < 100000000 -> 8
        else -> 9
    }
}

fun StringBuilder.appendPaddedTwoDigit(number: Int) {
    require(number in 0..99) { "Number is out of range [0; 99]" }

    val d1 = number / 10
    val d2 = number - d1 * 10

    append('0' + d1)
    append('0' + d2)
}

fun StringBuilder.appendPaddedFourDigit(number: Int) {
    require(number in 0..9999) { "Number is out of range [0; 9999]" }

    var t = number
    val d1 = t / 1000
    t -= d1 * 1000

    val d2 = t / 100
    t -= d2 * 100

    val d3 = t / 10
    val d4 = t - d3 * 10

    append('0' + d1)
    append('0' + d2)
    append('0' + d3)
    append('0' + d4)
}

/**
 * Parses string in range [start] to [end] (inclusive) to only positive
 * (actually non-negative, it can parse 0 as well) number.
 *
 * If format of the string region is invalid, it returns -1.
 */
fun CharSequence.parsePositiveInt(start: Int = 0, end: Int = length): Int {
    if (start == end) {
        return -1
    }

    var number = 0

    for (i in start until end) {
        val d = this[i] - '0'

        if (d !in 0..9) {
            return -1
        }

        number = number * 10 + d
    }

    return number
}

/**
 * Effectively creates subsequence of receiver [CharSequence] and converts it to [String].
 */
fun CharSequence.subSequenceToString(start: Int, end: Int): String {
    return when (this) {
        is String -> substring(start, end)
        is GetChars -> {
            val buffer = CharArray(end - start)
            getChars(start, end, buffer, 0)

            return String(buffer)
        }
        else -> subSequence(start, end).toString()
    }
}

/**
 * Trims receiver [CharSequence] from both start and end, returns result as a [String] instance.
 */
fun CharSequence?.trimToString(): String {
    if (this == null) {
        return ""
    }

    val length = length
    if (length == 0) {
        return ""
    }

    var start = 0
    var end = length

    for (i in 0 until length) {
        if (!this[i].isWhitespace()) {
            break
        }
        start++
    }

    var i = length - 1
    while (i > start) {
        if (!this[i].isWhitespace()) {
            break
        }

        end--
        i--
    }

    return subSequenceToString(start, end)
}

/**
 * Finds index of specified [char] in the receiver [CharSequence].
 * Search starts with [start] index and ends with [end] index.
 * If [char] is not found, returns `-1`
 */
fun CharSequence.indexOf(char: Char, start: Int, end: Int): Int {
    for (i in start until end) {
        if (this[i] == char) {
            return i
        }
    }

    return -1
}

fun CharSequence.getTrimRangeNonLetterOrDigit(start: Int, end: Int): PackedIntRange {
    if (start == end) {
        return PackedIntRange(start, end)
    }

    var rangeStart = start
    var rangeEnd = end

    for (i in start until end) {
        if (this[i].isLetterOrDigit()) {
            break
        }

        rangeStart++
    }

    var i = length - 1
    while (i > rangeStart) {
        if (this[i].isLetterOrDigit()) {
            break
        }

        rangeEnd--
        i--
    }

    return PackedIntRange(rangeStart, rangeEnd)
}

/**
 * Appends processed subsequence of [text] in range `[start; end)`.
 * The main idea of the 'processing' is to leave only meaningful parts of the sequence joined with spaces.
 * Example: " .. ; . AA  BB     CC DD ;;;" becomes "AA BB CC DD"
 *
 * The processing of the sequence:
 * - (1) Firstly, it determines the range in which
 *   all trailing and leading characters that are not letters or digits are not present
 *   (It's determined by [Char.isLetterOrDigit]. The method is ICU dependent,
 *   so on different versions of Android it can possibly returns different results)
 * - (2) Then, it appends the sequence to the [StringBuilder] char by char until it finds a character that isn't letter or digit.
 * - (3) If the 'illegal' character is found, it tries to find the next letter or digit character.
 *   Then it moves the internal offset to that index and appends space. By doing so, we can exclude following whitespaces
 *   from the result. Move to (2)
 */
fun StringBuilder.appendReducedNonLettersOrDigitsReplacedToSpace(
    text: CharSequence,
    start: Int,
    end: Int
) {
    val (trimmedStart, trimmedEnd) = text.getTrimRangeNonLetterOrDigit(start, end)

    var index = trimmedStart

    while (index < trimmedEnd) {
        val current = text[index]

        if (!current.isLetterOrDigit()) {
            index++

            while (index < trimmedEnd) {
                if (text[index].isLetterOrDigit()) {
                    break
                }

                index++
            }

            append(' ')
        } else {
            append(current)

            index++
        }
    }
}

/**
 * Fast-path for `buildString { appendReducedNonLettersOrDigitsReplacedToSpace(str, 0, str.length) }`
 */
fun CharSequence.reduceNonLettersOrDigitsReplacedToSpace(): String {
    val length = length
    val sb = StringBuilder(length)

    sb.appendReducedNonLettersOrDigitsReplacedToSpace(this, 0, length)

    return sb.toString()
}

fun String.containsLetterOrDigit() = any { it.isLetterOrDigit() }

fun createNumberRangeList(start: Int, endInclusive: Int, step: Int = 1): List<String> {
    if (start > endInclusive) {
        throw IllegalArgumentException("start > endInclusive")
    }

    var current = start

    // +1 because endInclusive is inclusive.
    val count = (endInclusive - start) / step + 1

    val list = ArrayList<String>(count)

    while (current <= endInclusive) {
        // Note: Integer.toString returns cached String instance if the value is within range (-100; 100).
        // In most cases the method is called with start and endInclusive being within that range.
        list.add(current.toString())
        current += step
    }

    return list
}