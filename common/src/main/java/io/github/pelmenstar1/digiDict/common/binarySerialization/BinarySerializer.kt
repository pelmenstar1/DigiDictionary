package io.github.pelmenstar1.digiDict.common.binarySerialization

import android.util.SparseArray

interface BinarySerializer<T : Any> {
    fun newArrayOfNulls(size: Int): Array<T?>

    fun writeTo(writer: PrimitiveValueWriter, value: T, compatInfo: BinarySerializationCompatInfo)
    fun readFrom(reader: PrimitiveValueReader, compatInfo: BinarySerializationCompatInfo): T
}

interface BinarySerializerResolver<T : Any> {
    val latest: BinarySerializer<T>
    val latestVersion: Int

    fun get(version: Int): BinarySerializer<T>?
}

/**
 * Represents an implementation of [BinarySerializerResolver] that uses [SparseArray] to store all known serializers.
 * The key of the [SparseArray] instance is a version of serializer.
 */
class SparseArrayBinarySerializerResolver<T : Any>(
    private val serializers: SparseArray<BinarySerializer<T>>
) : BinarySerializerResolver<T> {
    // As elements is sorted by key in SparseArray, element with the greatest key (version) should be the last.
    override val latest: BinarySerializer<T> = serializers.valueAt(serializers.size() - 1)

    override val latestVersion = serializers.keyAt(serializers.size() - 1)

    override fun get(version: Int): BinarySerializer<T>? {
        return serializers[version]
    }
}

@JvmInline
value class SparseArrayBinarySerializerResolverBuilder<T : Any>(
    private val serializers: SparseArray<BinarySerializer<T>>
) {
    fun register(version: Int, serializer: BinarySerializer<T>) {
        serializers.put(version, serializer)
    }

    /**
     * Assigns a [BinarySerializer] instance with specified [write] and [read] methods to given version.
     * [TR] is used to create new array of [T], so [TR] should be the same type as [T].
     * It's used to workaround type erasure.
     */
    inline fun <reified TR : T> register(
        version: Int,
        crossinline write: PrimitiveValueWriter.(T, BinarySerializationCompatInfo) -> Unit,
        crossinline read: PrimitiveValueReader.(compatInfo: BinarySerializationCompatInfo) -> T
    ) {
        val serializer = object : BinarySerializer<T> {
            @Suppress("UNCHECKED_CAST")
            override fun newArrayOfNulls(size: Int) = arrayOfNulls<TR>(size) as Array<T?>

            override fun writeTo(writer: PrimitiveValueWriter, value: T, compatInfo: BinarySerializationCompatInfo) {
                writer.write(value, compatInfo)
            }

            override fun readFrom(reader: PrimitiveValueReader, compatInfo: BinarySerializationCompatInfo): T {
                return reader.read(compatInfo)
            }
        }

        register(version, serializer)
    }
}

inline fun <T : Any> BinarySerializerResolver(
    block: SparseArrayBinarySerializerResolverBuilder<T>.() -> Unit
): BinarySerializerResolver<T> {
    val serializers = SparseArray<BinarySerializer<T>>(4)
    SparseArrayBinarySerializerResolverBuilder(serializers).block()

    return SparseArrayBinarySerializerResolver(serializers)
}