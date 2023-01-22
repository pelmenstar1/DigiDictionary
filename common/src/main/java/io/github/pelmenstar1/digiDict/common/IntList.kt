package io.github.pelmenstar1.digiDict.common

/**
 * Represents a simple list of [Int]'s. The main difference from `ArrayList<Int>` is that the [IntList] doesn't box the elements,
 * it uses direct approach using [IntArray].
 */
class IntList(capacity: Int = 0) {
    private var elements = IntArray(capacity)

    /**
     * Gets or sets current size of the list.
     * The size should not be greater than current capacity of the list, or negative, otherwise a [IllegalArgumentException] will be thrown.
     */
    var size = 0
        set(value) {
            if (value < 0) {
                throw IllegalArgumentException("Size can't be negative")
            }

            if (value > elements.size) {
                throw IllegalArgumentException("Size can't be greater than capacity of the list")
            }

            field = value
        }

    /**
     * Gets the element at specified [index].
     */
    operator fun get(index: Int): Int {
        checkIndex(index)

        return elements[index]
    }

    /**
     * Sets the [element] at specified [index]
     */
    operator fun set(index: Int, element: Int) {
        checkIndex(index)

        elements[index] = element
    }

    private fun checkIndex(index: Int) {
        if (index >= size) {
            throw IndexOutOfBoundsException("index: $index, size: $size")
        }
    }

    fun add(element: Int) {
        elements = PrimitiveListHelper.addLast(elements, size, element)
        size++
    }

    /**
     * Returns the underlying array of the list. The method does not allocate anything but the returned array is
     * unsafe to use as the list uses this array too.
     */
    fun getUnderlyingArray(): IntArray {
        return elements
    }
}