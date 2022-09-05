package io.github.pelmenstar1.digiDict.common.binarySerialization

class BinarySerializationStaticInfo(
    val sectionInfo: SectionsInfo,
    val keyResolverPairs: Array<out KeyResolverPair<out Any>>
) {
    interface SectionsInfo {
        val count: Int
    }

    class SectionKey<TValue : Any>(val ordinal: Int)
    class KeyResolverPair<TValue : Any>(
        val key: SectionKey<TValue>,
        val resolver: BinarySerializerResolver<TValue>
    )

    @Suppress("UNCHECKED_CAST")
    operator fun <TValue : Any> get(key: SectionKey<TValue>): BinarySerializerResolver<TValue> {
        val index = indexOf(key)
        if (index < 0) {
            throw IllegalStateException("A binary serializer resolver with given key is not found")
        }

        return keyResolverPairs[index].resolver as BinarySerializerResolver<TValue>
    }

    fun indexOf(key: SectionKey<*>): Int {
        return keyResolverPairs.indexOfFirst { it.key == key }
    }
}

fun <TValue : Any> BinarySerializationStaticInfo.SectionsInfo.key(ordinal: Int): BinarySerializationStaticInfo.SectionKey<TValue> {
    return BinarySerializationStaticInfo.SectionKey(ordinal)
}

infix fun <TValue : Any> BinarySerializerResolver<TValue>.connectedWith(
    key: BinarySerializationStaticInfo.SectionKey<TValue>
): BinarySerializationStaticInfo.KeyResolverPair<TValue> {
    return BinarySerializationStaticInfo.KeyResolverPair(key, this)
}

fun BinarySerializationStaticInfo(
    sections: BinarySerializationStaticInfo.SectionsInfo,
    vararg keyResolverPairs: BinarySerializationStaticInfo.KeyResolverPair<out Any>
): BinarySerializationStaticInfo {
    return BinarySerializationStaticInfo(sections, keyResolverPairs)
}