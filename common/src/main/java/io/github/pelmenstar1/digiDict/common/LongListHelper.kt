package io.github.pelmenstar1.digiDict.common

object LongListHelper {
    private fun newArraySize(currentSize: Int): Int {
        return if (currentSize == 0) {
            4
        } else {
            currentSize + (currentSize / 2)
        }
    }

    fun addLast(elements: LongArray, size: Int, element: Long): LongArray {
        var currentElements = elements

        if (size == elements.size) {
            val newElements = LongArray(newArraySize(size))
            System.arraycopy(elements, 0, newElements, 0, size)
            currentElements = newElements
        }

        currentElements[size] = element

        return currentElements
    }

    fun addFirst(elements: LongArray, size: Int, element: Long): LongArray {
        return if (elements.size == size) {
            val newElements = LongArray(newArraySize(size))
            newElements[0] = element
            System.arraycopy(elements, 0, newElements, 1, size)

            newElements
        } else {
            System.arraycopy(elements, 0, elements, 1, size)
            elements[0] = element

            elements
        }
    }
}