package io.github.pelmenstar1.digiDict.common.serialization

import android.util.SparseArray

interface BinarySerializer<T : Any> {
    fun newArrayOfNulls(size: Int): Array<T?>

    fun getByteSize(value: T): Int

    fun writeTo(writer: ValueWriter, value: T)
    fun readFrom(reader: ValueReader): T
}

interface BinarySerializerResolver<T : Any> {
    val latest: BinarySerializer<T>

    fun getOrLatest(version: Int): BinarySerializer<T>
}

class BinarySerializerResolverBuilder<T : Any> {
    private val serializers = SparseArray<BinarySerializer<T>>(4)

    fun forVersion(version: Int, serializer: BinarySerializer<T>) {
        serializers.put(version, serializer)
    }

    /**
     * Assigns a [BinarySerializer] instance with specified [getByteSize], [write] and [read] methods to given version.
     * [TR] is used to create new array of [T], so [TR] should be the same type as [T].
     * It's used to workaround type erasure.
     */
    inline fun <reified TR : T> forVersion(
        version: Int,
        crossinline getByteSize: BinarySize.(T) -> Int,
        crossinline write: ValueWriter.(T) -> Unit,
        crossinline read: ValueReader.() -> T
    ) {
        val serializer = object : BinarySerializer<T> {
            @Suppress("UNCHECKED_CAST")
            override fun newArrayOfNulls(size: Int): Array<T?> {
                return arrayOfNulls<TR>(size) as Array<T?>
            }

            override fun getByteSize(value: T) = BinarySize.getByteSize(value)

            override fun writeTo(writer: ValueWriter, value: T) {
                writer.write(value)
            }

            override fun readFrom(reader: ValueReader) = reader.read()
        }

        forVersion(version, serializer)
    }

    fun build() = object : BinarySerializerResolver<T> {
        override val latest: BinarySerializer<T>
            // As elements is sorted by key in SparseArray, element with the greatest key (version) should be the last.
            get() = serializers.valueAt(serializers.size() - 1)

        override fun getOrLatest(version: Int): BinarySerializer<T> {
            return serializers[version] ?: latest
        }
    }
}

inline fun <T : Any> MultiVersionBinarySerializerResolver(
    block: BinarySerializerResolverBuilder<T>.() -> Unit
): BinarySerializerResolver<T> {
    val builder = BinarySerializerResolverBuilder<T>().also(block)

    return builder.build()
}