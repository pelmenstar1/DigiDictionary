package io.github.pelmenstar1.digiDict.common

import android.os.Parcel
import android.os.Parcelable
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import java.util.*

class FixedBitSet : Parcelable {
    private val words: LongArray

    val size: Int

    constructor(size: Int) {
        this.size = size

        val wordCount = (size + WORD_BITS_COUNT - 1) shr WORD_SIZE

        words = LongArray(wordCount)
    }

    constructor(parcel: Parcel) {
        size = parcel.readInt()
        words = parcel.createLongArray() ?: EmptyArray.LONG
    }

    private constructor(words: LongArray, size: Int) {
        this.words = words
        this.size = size
    }

    override fun writeToParcel(dest: Parcel, flags: Int) = dest.run {
        writeInt(size)
        writeLongArray(words)
    }

    override fun describeContents() = 0

    operator fun get(index: Int): Boolean {
        checkIndex(index)

        val word = words[getWordIndex(index)]
        return (word and (1L shl index)) != 0L
    }

    fun set(index: Int) {
        checkIndex(index)

        val wordIndex = getWordIndex(index)
        words[wordIndex] = words[wordIndex] or (1L shl index)
    }

    operator fun set(index: Int, state: Boolean) {
        checkIndex(index)

        val wordIndex = getWordIndex(index)
        val word = words[wordIndex]
        val mask = 1L shl index

        words[wordIndex] = if (state) {
            word or mask
        } else {
            word and mask.inv()
        }
    }

    fun setAll(state: Boolean) {
        Arrays.fill(words, if (state) -1L else 0L)
    }

    fun setAll() {
        Arrays.fill(words, -1L)
    }

    fun isAllBitsSet(): Boolean {
        val size = size
        val words = words

        // Edge case: If there are no bits in the set, then all bits are set.
        if (size == 0) {
            return true
        }

        val fullWordCount = size shr WORD_SIZE
        var concatMask = -1L

        for (i in 0 until fullWordCount) {
            concatMask = concatMask and words[i]
        }

        if (concatMask != -1L) {
            return false
        }

        val remainingN = size - (fullWordCount shl WORD_SIZE)
        if (remainingN > 0) {
            return words[words.size - 1].countOneBits() == remainingN
        }

        return true
    }

    private fun checkIndex(index: Int) {
        if (index < 0 || index >= size) {
            throw IndexOutOfBoundsException("index=$index; size=$size")
        }
    }

    override fun equals(other: Any?) = equalsPattern(other) { o ->
        size == o.size && words.contentEquals(o.words)
    }

    override fun hashCode(): Int {
        var result = size
        result = result * 31 + words.contentHashCode()

        return result
    }

    override fun toString(): String {
        val setBitsText = buildString {
            append('[')
            words.iterateSetBits { index ->
                append(index)
                append(',')
            }
            append(']')
        }

        return "FixedBitSet(size=$size; setBits=$setBitsText)"
    }

    companion object {
        private const val WORD_SIZE = 6
        private const val WORD_BITS_COUNT = 64

        val EMPTY = FixedBitSet(EmptyArray.LONG, 0)

        @JvmField
        val CREATOR = object : Parcelable.Creator<FixedBitSet> {
            override fun createFromParcel(source: Parcel) = FixedBitSet(source)
            override fun newArray(size: Int) = arrayOfNulls<FixedBitSet>(size)
        }

        internal fun getWordIndex(index: Int) = index shr WORD_SIZE
    }
}

fun Int.withBit(mask: Int, state: Boolean): Int {
    return if (state) this or mask else this and mask.inv()
}

fun Int?.withBit(mask: Int, state: Boolean): Int {
    val value = this ?: 0

    return value.withBit(mask, state)
}

fun MutableStateFlow<Int?>.withBitNullable(mask: Int, state: Boolean) {
    update { it.withBit(mask, state) }
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
    var countOfBitsUntilTarget = n

    // This loop tries to find a word which contains n-th bit.
    for (i in indices) {
        val element = this[i]
        val bitCount = element.countOneBits()

        // If element contains less bits than countOfBitsUntilTarget, element is not the target.
        if (countOfBitsUntilTarget >= bitCount) {
            countOfBitsUntilTarget -= bitCount
        } else {
            val index = element.findPositionOfNthSetBit(countOfBitsUntilTarget)

            // As 'index' is within [0; 64) range, it needs to be translated to bitset-wide index
            // by multiplying index of the target by 64 (word size) and adding word-wide index.
            return (i shl 6) + index
        }
    }

    return -1
}

inline fun LongArray.iterateSetBits(block: (bitIndex: Int) -> Unit) {
    for (i in indices) {
        val element = this[i]

        // Multiply by 64
        val baseIndex = i shl 6

        element.iterateSetBits { bitIndex ->
            block(baseIndex + bitIndex)
        }
    }
}

// The implementation was taken from OpenJDK
// (https://github.com/openjdk/jdk/blob/master/src/java.base/share/classes/java/util/BitSet.java)
// and converted to Kotlin.
fun LongArray.nextSetBit(fromIndex: Int): Int {
    if (fromIndex < 0) {
        throw IllegalArgumentException("fromIndex < 0 (fromIndex=$fromIndex)")
    }

    val size = size
    var wordIndex = fromIndex shr 6

    if (wordIndex >= size) {
        return -1
    }

    var word = this[wordIndex] and (-1L shl fromIndex)

    while (true) {
        if (word != 0L) {
            return wordIndex * 64 + word.countTrailingZeroBits()
        }

        if (++wordIndex == size) {
            return -1
        }

        word = this[wordIndex]
    }
}

private inline fun <T> T.getByteAt(index: Int, shr: T.(bitCount: Int) -> T, toByte: T.() -> Byte): Byte {
    return this.shr(index shl 3).toByte()
}

fun Short.getByteAt(index: Int) = toInt().getByteAt(index)
fun Int.getByteAt(index: Int) = getByteAt(index, Int::shr, Int::toByte)
fun Long.getByteAt(index: Int) = getByteAt(index, Long::shr, Long::toByte)

fun Short.writeTo(dest: ByteArray, offset: Int) {
    dest[offset] = this.toByte()
    dest[offset + 1] = (this.toInt() shr 8).toByte()
}

fun Int.writeTo(dest: ByteArray, offset: Int) {
    dest[offset] = this.toByte()
    dest[offset + 1] = (this shr 8).toByte()
    dest[offset + 2] = (this shr 16).toByte()
    dest[offset + 3] = (this shr 24).toByte()
}

fun Long.writeTo(dest: ByteArray, offset: Int) {
    dest[offset] = this.toByte()
    dest[offset + 1] = (this shr 8).toByte()
    dest[offset + 2] = (this shr 16).toByte()
    dest[offset + 3] = (this shr 24).toByte()
    dest[offset + 4] = (this shr 32).toByte()
    dest[offset + 5] = (this shr 40).toByte()
    dest[offset + 6] = (this shr 48).toByte()
    dest[offset + 7] = (this shr 56).toByte()
}

fun ByteArray.readShort(offset: Int): Short {
    return ((this[offset].toInt() and 0xFF) or
            (this[offset + 1].toInt() and 0xFF shl 8)).toShort()
}

fun ByteArray.readInt(offset: Int): Int {
    return (this[offset].toInt() and 0xFF) or
            (this[offset + 1].toInt() and 0xFF shl 8) or
            (this[offset + 2].toInt() and 0xFF shl 16) or
            (this[offset + 3].toInt() and 0xFF shl 24)
}

fun ByteArray.readLong(offset: Int): Long {
    return (this[offset].toLong() and 0xFF) or
            (this[offset + 1].toLong() and 0xFF shl 8) or
            (this[offset + 2].toLong() and 0xFF shl 16) or
            (this[offset + 3].toLong() and 0xFF shl 24) or
            (this[offset + 4].toLong() and 0xFF shl 32) or
            (this[offset + 5].toLong() and 0xFF shl 40) or
            (this[offset + 6].toLong() and 0xFF shl 48) or
            (this[offset + 7].toLong() and 0xFF shl 56)
}