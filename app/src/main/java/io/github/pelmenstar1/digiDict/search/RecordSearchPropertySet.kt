package io.github.pelmenstar1.digiDict.search

import android.os.Parcel
import android.os.Parcelable
import io.github.pelmenstar1.digiDict.common.equalsPattern
import io.github.pelmenstar1.digiDict.common.isLastBit
import io.github.pelmenstar1.digiDict.common.iterateSetBits

class RecordSearchPropertySet : Set<RecordSearchProperty>, Parcelable {
    internal val ordinalBits: Int

    override val size: Int
        get() = ordinalBits.countOneBits()

    private constructor(bits: Int) {
        this.ordinalBits = bits
    }

    constructor(elements: Array<out RecordSearchProperty>) {
        var b = 0
        for (element in elements) {
            b = b or (1 shl element.ordinal)
        }

        ordinalBits = b
    }

    constructor(parcel: Parcel) {
        ordinalBits = parcel.readInt()
    }

    override fun isEmpty(): Boolean = ordinalBits == 0

    override fun contains(element: RecordSearchProperty): Boolean {
        return (ordinalBits and (1 shl element.ordinal)) != 0
    }

    override fun containsAll(elements: Collection<RecordSearchProperty>): Boolean {
        return elements.all { contains(it) }
    }

    override fun iterator(): Iterator<RecordSearchProperty> = IteratorImpl(ordinalBits)

    fun toOrdinalArray(): IntArray {
        val result = IntArray(size)
        var resultIndex = 0

        ordinalBits.iterateSetBits { ordinal ->
            result[resultIndex++] = ordinal
        }

        return result
    }

    override fun equals(other: Any?) = equalsPattern(other) { o ->
        ordinalBits == o.ordinalBits
    }

    override fun hashCode(): Int = ordinalBits

    override fun toString(): String {
        val bits = ordinalBits
        val allElements = ALL_ELEMENTS

        return buildString {
            append("RecordSearchPropertySet[")

            bits.iterateSetBits { ordinal ->
                append(allElements[ordinal].name)

                if (!bits.isLastBit(ordinal)) {
                    append(", ")
                }
            }

            append(']')
        }
    }

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeInt(ordinalBits)
    }

    override fun describeContents(): Int = 0

    private class IteratorImpl(private var bits: Int) : Iterator<RecordSearchProperty> {
        override fun hasNext(): Boolean {
            return bits != 0
        }

        override fun next(): RecordSearchProperty {
            val b = bits
            val elements = ALL_ELEMENTS

            for (i in elements.indices) {
                val mask = 1 shl i

                if ((b and mask) != 0) {
                    bits = b and mask.inv()

                    return elements[i]
                }
            }

            throw IllegalStateException("No elements left")
        }
    }

    companion object {
        private val ALL_ELEMENTS = RecordSearchProperty.values()

        private const val ALL_ELEMENTS_BITS = 0x3

        private val ALL = RecordSearchPropertySet(ALL_ELEMENTS_BITS)

        fun all(): RecordSearchPropertySet = ALL

        @JvmField
        val CREATOR = object : Parcelable.Creator<RecordSearchPropertySet> {
            override fun createFromParcel(parcel: Parcel) = RecordSearchPropertySet(parcel)
            override fun newArray(size: Int) = arrayOfNulls<RecordSearchPropertySet>(size)
        }
    }
}