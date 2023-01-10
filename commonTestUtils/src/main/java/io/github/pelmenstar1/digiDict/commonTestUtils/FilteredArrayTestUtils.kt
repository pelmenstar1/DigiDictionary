package io.github.pelmenstar1.digiDict.commonTestUtils

object FilteredArrayTestUtils {
    fun createBitSet(size: Int, indices: IntArray?): LongArray {
        val wordCount = (size + 63) / 64
        val bitSet = LongArray(wordCount)

        val iterator = indices?.iterator() ?: (0 until size).iterator()

        for (index in iterator) {
            val wordIndex = index / 64
            bitSet[wordIndex] = bitSet[wordIndex] or (1L shl index)
        }

        return bitSet
    }
}