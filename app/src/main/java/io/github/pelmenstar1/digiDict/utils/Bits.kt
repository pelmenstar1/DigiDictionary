package io.github.pelmenstar1.digiDict.utils

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update

fun lowestNBitsSet(n: Int): Int {
    // Special case: Shift operator takes into account only lowest 5 bits which means
    // if left-shift 0xFFFFFFFF by >= 32, result will be wrong. So return 0xFFFFFFFF (-1)
    // which is most appropriate value in such case
    if (n >= 32) return -1

    // -1 is number with all bit set.
    // Then it's left-shifted by n, to get a number which has n lowest bits unset while the others are set.
    // In order to get desired number we invert it and get the number which has n lowest bits set.
    return ((-1) shl n).inv()
}

fun Int.withBitAtPosition(position: Int, state: Boolean): Int {
    return withBit(1 shl position, state)
}

fun Int.withBit(mask: Int, state: Boolean): Int {
    return if (state) this or mask else this and mask.inv()
}

fun MutableStateFlow<Int>.withBit(mask: Int, state: Boolean) {
    update { it.withBit(mask, state) }
}

fun Int.isBitAtPositionSet(position: Int): Boolean {
    return (this and (1 shl position)) != 0
}

// Finds such a position S, that range [0; S] of bitSet has N set bits.
fun Long.findPositionOfNthSetBit(n: Int): Int {
    var seqIndex = 0

    iterateSetBits { bitIndex ->
        if (seqIndex == n) {
            return bitIndex
        }

        seqIndex++
    }

    return -1
}

inline fun Long.iterateSetBits(block: (bitIndex: Int) -> Unit) {
    // Original source: https://lemire.me/blog/2018/02/21/iterating-over-set-bits-quickly/

    var bits = this

    while (bits != 0L) {
        val t = bits and (-bits)
        val bitIndex = t.countLeadingZeroBits()
        block(63 - bitIndex)

        bits = bits xor t
    }
}

// Finds such a position S, that range [0; S] in bitSet has N set bits.
fun LongArray.findPositionOfNthSetBit(n: Int): Int {
    var remainingN = n

    for (i in indices) {
        val element = this[i]
        val bitCount = element.countOneBits()

        // Skip a word if we know it doesn't contain appropriate amount of set bits (It has less bits than it's needed)
        if (bitCount <= remainingN) {
            remainingN -= bitCount
        } else {
            val index = element.findPositionOfNthSetBit(remainingN)

            return (i shl 6) + index
        }
    }

    return -1
}

fun LongArray.iterateSetBits(block: (bitIndex: Int) -> Unit) {
    for (i in indices) {
        val element = this[i]

        // Multiply by 64
        val baseIndex = i shl 6

        element.iterateSetBits { bitIndex ->
            block(baseIndex + bitIndex)
        }
    }
}