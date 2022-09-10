package io.github.pelmenstar1.digiDict.common.binarySerialization

class BinarySerializationStaticInfo<TKeys : BinarySerializationSectionKeys>(
    val keys: TKeys,
    val resolvers: Array<out BinarySerializerResolver<out Any>>
) {
    @Suppress("UNCHECKED_CAST")
    operator fun <TValue : Any> get(key: BinarySerializationSectionKey<TKeys, TValue>): BinarySerializerResolver<TValue> {
        return resolvers[key.ordinal] as BinarySerializerResolver<TValue>
    }
}

@JvmInline
value class BinarySerializationKeyResolverPairListBuilder<TKeys : BinarySerializationSectionKeys>(
    private val pairs: Array<BinarySerializerResolver<out Any>?>
) {
    fun <TValue : Any> section(
        key: BinarySerializationSectionKey<TKeys, TValue>,
        resolver: BinarySerializerResolver<TValue>
    ) {
        pairs[key.ordinal] = resolver
    }
}

@Suppress("UNCHECKED_CAST")
inline fun <TKeys : BinarySerializationSectionKeys> BinarySerializationStaticInfo(
    keys: TKeys,
    block: BinarySerializationKeyResolverPairListBuilder<TKeys>.() -> Unit
): BinarySerializationStaticInfo<TKeys> {
    val resolvers = arrayOfNulls<BinarySerializerResolver<out Any>>(keys.size)
    block(BinarySerializationKeyResolverPairListBuilder(resolvers))

    if (resolvers.any { it == null }) {
        throw IllegalStateException("All keys of given keys should be added the static info")
    }

    // Each element of resolvers is proved to be not-null, so the cast is safe.
    return BinarySerializationStaticInfo(keys, resolvers as Array<out BinarySerializerResolver<out Any>>)
}