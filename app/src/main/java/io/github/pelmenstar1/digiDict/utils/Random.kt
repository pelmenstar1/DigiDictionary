package io.github.pelmenstar1.digiDict.utils

import io.github.pelmenstar1.digiDict.EmptyArray
import kotlin.random.Random

/**
 * Generates unique random numbers.
 *
 * @param upperBound upper bound of generated range.
 * @param size length of the array.
 */
fun Random.generateUniqueRandomNumbers(upperBound: Int, size: Int): IntArray {
    if (upperBound < size) {
        throw IllegalArgumentException("upperBounds < size; upperBound=$upperBound, size=$size")
    }

    if (size == 0 || upperBound == 0) {
        return EmptyArray.INT
    }

    val numbers = IntArray(size)

    var i = 0

    while (i < size) {
        val number = nextInt(upperBound)

        if (!numbers.contains(number, 0, i)) {
            numbers[i] = number
            i++
        }
    }

    return numbers
}