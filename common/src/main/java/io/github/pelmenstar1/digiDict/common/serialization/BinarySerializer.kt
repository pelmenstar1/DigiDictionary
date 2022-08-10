package io.github.pelmenstar1.digiDict.common.serialization

interface BinarySerializer<T : Any> {
    fun getByteSize(value: T): Int

    fun writeTo(writer: ValueWriter, value: T)
    fun readFrom(reader: ValueReader): T
}