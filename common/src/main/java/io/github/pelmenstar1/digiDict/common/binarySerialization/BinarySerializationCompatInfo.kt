package io.github.pelmenstar1.digiDict.common.binarySerialization

import io.github.pelmenstar1.digiDict.common.equalsPattern

/**
 * Stores a raw information of compatibility info packed as 64-bit int.
 */
class BinarySerializationCompatInfo(val bits: Long) {
    /**
     * Gets whether bit at given [index] is set.
     */
    operator fun get(index: Int): Boolean {
        require(index in 0 until 64) { "Index is out of bounds" }

        return (bits and (1L shl index)) != 0L
    }

    override fun equals(other: Any?) = equalsPattern(other) { o -> bits == o.bits }
    override fun hashCode(): Int = (bits and (bits ushr 32)).toInt()

    override fun toString(): String {
        return "BinarySerializationCompatInfo(bits=$bits})"
    }

    companion object {
        private val EMPTY = BinarySerializationCompatInfo(0L)

        fun empty() = EMPTY
    }
}

class BinarySerializationCompatInfoBuilder {
    private var bits = 0L

    /**
     * Sets the bit at given [index].
     */
    fun set(index: Int) {
        require(index in 0 until 64) { "Index is out of bounds" }

        bits = bits or (1L shl index)
    }

    fun create() = BinarySerializationCompatInfo(bits)
}

inline fun BinarySerializationCompatInfo(block: BinarySerializationCompatInfoBuilder.() -> Unit): BinarySerializationCompatInfo {
    return BinarySerializationCompatInfoBuilder().also(block).create()
}