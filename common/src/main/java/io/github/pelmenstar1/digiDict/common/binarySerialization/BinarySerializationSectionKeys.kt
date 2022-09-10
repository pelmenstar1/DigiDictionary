package io.github.pelmenstar1.digiDict.common.binarySerialization

interface BinarySerializationSectionKeys {
    val size: Int
}

class BinarySerializationSectionKey<TKeys : BinarySerializationSectionKeys, TValue : Any>(val ordinal: Int)

// Receiver parameter is effectively unused, but it makes possible to narrow the range of using concise creation of BinarySerializationStaticInfo.SectionKey
@Suppress("unused")
fun <TKeys : BinarySerializationSectionKeys, TValue : Any> TKeys.key(ordinal: Int): BinarySerializationSectionKey<TKeys, TValue> {
    return BinarySerializationSectionKey(ordinal)
}