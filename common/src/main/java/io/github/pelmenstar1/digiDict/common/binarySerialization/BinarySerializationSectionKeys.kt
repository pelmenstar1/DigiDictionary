package io.github.pelmenstar1.digiDict.common.binarySerialization

import io.github.pelmenstar1.digiDict.common.getLazyValue

class BinarySerializationSectionKey<TKeys : BinarySerializationSectionKeys<TKeys>, TValue : Any>(
    val ordinal: Int,
    val name: String
)

interface BinarySerializationSectionKeys<TSelf : BinarySerializationSectionKeys<TSelf>> {
    val size: Int

    operator fun get(ordinal: Int): BinarySerializationSectionKey<TSelf, out Any>
}

abstract class SimpleBinarySerializationSectionKeys<TSelf : SimpleBinarySerializationSectionKeys<TSelf>> :
    BinarySerializationSectionKeys<TSelf> {
    private var allHolder: Array<out BinarySerializationSectionKey<TSelf, out Any>>? = null

    final override val size: Int
        get() = getAllHolderValue().size

    final override fun get(ordinal: Int): BinarySerializationSectionKey<TSelf, out Any> {
        return getAllHolderValue()[ordinal]
    }

    protected abstract fun getAll(): Array<out BinarySerializationSectionKey<TSelf, out Any>>

    protected fun <TValue : Any> key(ordinal: Int, name: String): BinarySerializationSectionKey<TSelf, TValue> {
        return BinarySerializationSectionKey(ordinal, name)
    }

    private fun getAllHolderValue(): Array<out BinarySerializationSectionKey<TSelf, out Any>> {
        return getLazyValue(allHolder, ::getAll) { allHolder = it }
    }
}