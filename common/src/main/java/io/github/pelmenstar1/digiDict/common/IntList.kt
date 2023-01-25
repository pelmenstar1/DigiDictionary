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
     * Increases the capacity of the list to ensure
     * it can hold at least the number of elements specified by [newCapacity].
     */
    fun ensureCapacity(newCapacity: Int) {
        if (newCapacity < 0) {
            throw IllegalArgumentException("newCapacity can't be negative")
        }

        val elements = elements

        if (newCapacity > elements.size) {
            val newElements = IntArray(PrimitiveListHelper.newArraySize(newCapacity))
            System.arraycopy(elements, 0, newElements, 0, size)

            this.elements = newElements
        }
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

    /**
     * Adds specified [element] to the end of the list.
     */
    fun add(element: Int) {
        elements = PrimitiveListHelper.addLast(elements, size, element)
        size++
    }

    /**
     * Returns the [IntArray] copy of the list.
     */
    fun toArray(): IntArray {
        val newElements = IntArray(size)
        System.arraycopy(elements, 0, newElements, 0, newElements.size)

        return elements
    }
}