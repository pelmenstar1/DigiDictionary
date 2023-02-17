@file:Suppress("NOTHING_TO_INLINE")

package io.github.pelmenstar1.digiDict.common

import android.text.GetChars

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

private fun validateTwoDigitNumber(number: Int) {
    require(number in 0..99) { "Number is out of range [0; 99]" }
}

private inline fun paddedTwoDigitHelper(number: Int, block: (d1: Char, d2: Char) -> Unit) {
    validateTwoDigitNumber(number)

    val d1 = number / 10
    val d2 = number - d1 * 10

    block('0' + d1, '0' + d2)
}

fun StringBuilder.appendPaddedTwoDigit(number: Int) {
    paddedTwoDigitHelper(number) { d1, d2 ->
        append(d1)
        append(d2)
    }
}

fun CharArray.writePaddedTwoDigit(number: Int, offset: Int) {
    paddedTwoDigitHelper(number) { d1, d2 ->
        this[offset] = d1
        this[offset + 1] = d2
    }
}

private fun validateFourDigitNumber(number: Int) {
    require(number in 0..9999) { "Number is out of range [0; 9999]" }
}

private inline fun paddedFourDigitHelper(number: Int, block: (d1: Char, d2: Char, d3: Char, d4: Char) -> Unit) {
    validateFourDigitNumber(number)

    var t = number
    val d1 = t / 1000
    t -= d1 * 1000

    val d2 = t / 100
    t -= d2 * 100

    val d3 = t / 10
    val d4 = t - d3 * 10

    block('0' + d1, '0' + d2, '0' + d3, '0' + d4)
}

fun StringBuilder.appendPaddedFourDigit(number: Int) {
    paddedFourDigitHelper(number) { d1, d2, d3, d4 ->
        append(d1)
        append(d2)
        append(d3)
        append(d4)
    }
}

fun CharArray.writePaddedFourDigit(number: Int, offset: Int) {
    paddedFourDigitHelper(number) { d1, d2, d3, d4 ->
        this[offset] = d1
        this[offset + 1] = d2
        this[offset + 2] = d3
        this[offset + 3] = d4
    }
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

private inline fun String.nextIndexWhen(start: Int, end: Int, predicate: (Char) -> Boolean): Int {
    for (i in start until end) {
        if (predicate(this[i])) {
            return i
        }
    }

    return -1
}

fun String.nextLetterOrDigitIndex(start: Int, end: Int) = nextIndexWhen(start, end) { it.isLetterOrDigit() }
fun String.nextNonLetterOrDigitIndex(start: Int, end: Int) = nextIndexWhen(start, end) { !it.isLetterOrDigit() }

fun String.containsLetterOrDigit() = nextLetterOrDigitIndex(0, length) >= 0

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

fun Any?.toStringOrEmpty(): String = this?.toString() ?: ""

fun CharSequence?.equalsByChar(other: CharSequence): Boolean {
    if (this == null) {
        return false
    }

    val thisLength = length
    val otherLength = other.length

    if (thisLength != otherLength) {
        return false
    }

    for (i in 0 until thisLength) {
        if (this[i] != other[i]) {
            return false
        }
    }

    return true
}