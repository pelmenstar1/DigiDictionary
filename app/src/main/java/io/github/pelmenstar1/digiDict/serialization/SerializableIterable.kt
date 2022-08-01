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

fun <T : Any> SerializableIterable(values: Array<out T>, serializer: BinarySerializer<in T>): SerializableIterable {
    return object : SerializableIterable {
        override val size: Int
            get() = values.size

        override fun iterator() = object : SerializableIterator {
            private var index = 0
            private var element: T? = null

            override fun moveToNext(): Boolean {
                if (index >= values.size) {
                    return false
                }

                element = values[index++]

                return true
            }

            override fun initCurrent() {
            }

            override fun getCurrentElementByteSize(): Int {
                return serializer.getByteSize(element!!)
            }

            override fun writeCurrentElement(writer: ValueWriter) {
                serializer.writeTo(writer, element!!)
            }
        }

        override fun recycle() {
        }
    }
}