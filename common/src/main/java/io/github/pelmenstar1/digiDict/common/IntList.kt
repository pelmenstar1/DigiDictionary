package io.github.pelmenstar1.digiDict.common

/**
 * Represents a simple list of [Int]'s. The main difference from `ArrayList<Int>` is that the [IntList] doesn't box the elements,
 * it uses direct approach using [IntArray].
 */
class IntList(capacity: Int = 0) {
    @PublishedApi
    internal var elements = IntArray(capacity)

    @PublishedApi
    internal var _size = 0

    /**
     * Gets or sets current size of the list.
     * The size should not be greater than current capacity of the list, or negative, otherwise a [IllegalArgumentException] will be thrown.
     */
    var size: Int
        get() = _size
        set(value) {
            if (value < 0) {
                throw IllegalArgumentException("Size can't be negative")
            }

            if (value > elements.size) {
                throw IllegalArgumentException("Size can't be greater than capacity of the list")
            }

            _size = value
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
            System.arraycopy(elements, 0, newElements, 0, _size)

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
        if (index >= _size) {
            throw IndexOutOfBoundsException("index: $index, size: $_size")
        }
    }

    /**
     * Returns whether the list contains specified [element].
     */
    fun contains(element: Int): Boolean {
        return elements.contains(element, 0, _size)
    }

    /**
     * Adds specified [element] to the end of the list.
     */
    fun add(element: Int) {
        elements = PrimitiveListHelper.addLast(elements, _size, element)
        _size++
    }

    /**
     * Adds specified [element] to the end of the list repeating it the number of times as specified in [count].
     */
    fun addRepeat(element: Int, count: Int) {
        when {
            count < 0 -> throw IllegalArgumentException("count can't be negative")
            count == 0 -> return
        }

        var listSize = _size
        ensureCapacity(listSize + count)

        val listElements = elements

        for (i in listSize until (listSize + count)) {
            listElements[i] = element
        }

        listSize += count
        _size = listSize
    }

    fun addRange(elements: IntArray) {
        val listSize = _size
        val elementsLength = elements.size

        if (elementsLength == 0) {
            return
        }

        ensureCapacity(listSize + elementsLength)
        System.arraycopy(elements, 0, this.elements, listSize, elementsLength)

        _size = listSize + elementsLength
    }

    inline fun <T> addMapped(elements: Array<out T>, transform: (T) -> Int) {
        val listSize = _size
        val listElements = this.elements
        val elementsLength = elements.size

        ensureCapacity(listSize + elementsLength)
        for (i in elements.indices) {
            listElements[listSize + i] = transform(elements[i])
        }

        _size = listSize + elementsLength
    }

    /**
     * Removes all the elements from the list. The internal buffer won't be cleared.
     */
    fun clear() {
        size = 0
    }

    /**
     * Returns the [IntArray] copy of the list.
     */
    fun toArray(): IntArray {
        val size = size

        val newElements = IntArray(size)
        System.arraycopy(elements, 0, newElements, 0, size)

        return newElements
    }
}