package io.github.pelmenstar1.digiDict.common.serialization

interface SerializableIterable {
    val size: Int
    val version: Int

    fun iterator(): SerializableIterator
    fun recycle()
}

interface SerializableIterator {
    fun moveToNext(): Boolean

    fun initCurrent()
    fun getCurrentElementByteSize(): Int
    fun writeCurrentElement(writer: ValueWriter)
}

fun <T : Any> SerializableIterable(
    values: Array<out T>,
    serializer: BinarySerializer<in T>,
    version: Int = 1
): SerializableIterable {
    return object : SerializableIterable {
        override val size: Int
            get() = values.size

        override val version: Int
            get() = version

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