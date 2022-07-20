package io.github.pelmenstar1.digiDict.serialization

interface BinarySerializer<T : Any> {
    fun newArray(n: Int): Array<T?>
    fun getByteSize(value: T): Int

    fun writeTo(writer: ValueWriter, value: T)
    fun readFrom(reader: ValueReader): T
}