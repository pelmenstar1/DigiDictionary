package io.github.pelmenstar1.digiDict.serialization

interface SerializableIterable {
    val size: Int

    fun iterator(): SerializableIterator
    fun recycle()
}

interface SerializableIterator {
    fun moveToNext(): Boolean

    fun initCurrent()
    fun getCurrentElementByteSize(): Int
    fun writeCurrentElement(writer: ValueWriter)
}