package io.github.pelmenstar1.digiDict.common

private fun newArraySize(currentSize: Int): Int {
    return if (currentSize == 0) {
        4
    } else {
        currentSize + (currentSize / 2)
    }
}

private inline fun <TArray, TElement> addLastInternal(
    elements: TArray,
    elementsLength: Int,
    size: Int,
    element: TElement,
    set: TArray.(index: Int, TElement) -> Unit,
    newArray: (size: Int) -> TArray
): TArray {
    var currentElements = elements

    if (size == elementsLength) {
        val newElements = newArray(newArraySize(size))

        // The lint doesn't understand that TArray is an array type
        @Suppress("NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS")
        System.arraycopy(elements, 0, newElements, 0, size)
        currentElements = newElements
    }

    currentElements.set(size, element)

    return currentElements
}

// The lint doesn't understand that TArray is an array type
@Suppress("NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS")
private inline fun <TArray, TElement> addFirstInternal(
    elements: TArray,
    elementsLength: Int,
    size: Int,
    element: TElement,
    set: TArray.(index: Int, TElement) -> Unit,
    newArray: (size: Int) -> TArray
): TArray {
    return if (elementsLength == size) {
        val newElements = newArray(newArraySize(size))
        newElements.set(0, element)
        System.arraycopy(elements, 0, newElements, 1, size)

        newElements
    } else {
        System.arraycopy(elements, 0, elements, 1, size)
        elements.set(0, element)

        elements
    }
}

object PrimitiveListHelper {
    fun addLast(elements: IntArray, size: Int, element: Int): IntArray {
        return addLastInternal(elements, elements.size, size, element, IntArray::set, ::IntArray)
    }

    fun addFirst(elements: IntArray, size: Int, element: Int): IntArray {
        return addFirstInternal(elements, elements.size, size, element, IntArray::set, ::IntArray)
    }

    fun addLast(elements: LongArray, size: Int, element: Long): LongArray {
        return addLastInternal(elements, elements.size, size, element, LongArray::set, ::LongArray)
    }

    fun addFirst(elements: LongArray, size: Int, element: Long): LongArray {
        return addFirstInternal(elements, elements.size, size, element, LongArray::set, ::LongArray)
    }
}