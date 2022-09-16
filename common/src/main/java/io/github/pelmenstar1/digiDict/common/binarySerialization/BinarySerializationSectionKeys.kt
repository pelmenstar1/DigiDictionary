package io.github.pelmenstar1.digiDict.common.binarySerialization

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
    private val all: Array<out BinarySerializationSectionKey<TSelf, out Any>>

    final override val size: Int
        get() = all.size

    init {
        @Suppress("LeakingThis")
        all = getAll()
    }

    final override fun get(ordinal: Int) = all[ordinal]

    protected fun <TValue : Any> key(ordinal: Int, name: String): BinarySerializationSectionKey<TSelf, TValue> {
        return BinarySerializationSectionKey(ordinal, name)
    }

    protected abstract fun getAll(): Array<out BinarySerializationSectionKey<TSelf, out Any>>
}