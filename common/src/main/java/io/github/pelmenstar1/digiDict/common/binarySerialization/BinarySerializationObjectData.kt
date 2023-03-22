package io.github.pelmenstar1.digiDict.common.binarySerialization

import io.github.pelmenstar1.digiDict.common.unsafeNewArray
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

class BinarySerializationObjectData<TKeys : BinarySerializationSectionKeys<TKeys>>(
    val staticInfo: BinarySerializationStaticInfo<TKeys>,
    val compatInfo: BinarySerializationCompatInfo,
    val sections: Array<out Array<out Any>>
) {
    init {
        require(staticInfo.keys.size == sections.size)
    }

    @Suppress("UNCHECKED_CAST")
    operator fun <TValue : Any> get(key: BinarySerializationSectionKey<TKeys, TValue>): Array<out TValue> {
        return sections[key.ordinal] as Array<out TValue>
    }

    fun <TValue : Any> get(key: TKeys.() -> BinarySerializationSectionKey<TKeys, TValue>): Array<out TValue> {
        contract {
            callsInPlace(key, InvocationKind.EXACTLY_ONCE)
        }

        return get(staticInfo.keys.key())
    }
}

@JvmInline
value class BinarySerializationObjectDataBuilder<TKeys : BinarySerializationSectionKeys<TKeys>>(
    private val sections: Array<Array<out Any>>
) {
    fun <TValue : Any> put(key: BinarySerializationSectionKey<TKeys, TValue>, values: Array<out TValue>) {
        sections[key.ordinal] = values
    }
}

inline fun <TKeys : BinarySerializationSectionKeys<TKeys>> BinarySerializationObjectData(
    staticInfo: BinarySerializationStaticInfo<TKeys>,
    compatInfo: BinarySerializationCompatInfo,
    block: BinarySerializationObjectDataBuilder<TKeys>.() -> Unit
): BinarySerializationObjectData<TKeys> {
    contract {
        callsInPlace(block, InvocationKind.EXACTLY_ONCE)
    }

    val sections = unsafeNewArray<Array<out Any>>(staticInfo.keys.size)
    BinarySerializationObjectDataBuilder<TKeys>(sections).block()

    return BinarySerializationObjectData(staticInfo, compatInfo, sections)
}