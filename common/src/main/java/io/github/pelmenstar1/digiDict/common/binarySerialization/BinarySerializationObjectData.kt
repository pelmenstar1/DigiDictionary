package io.github.pelmenstar1.digiDict.common.binarySerialization

import io.github.pelmenstar1.digiDict.common.ProgressReporter
import io.github.pelmenstar1.digiDict.common.trackLoopProgressWithSubReporters
import io.github.pelmenstar1.digiDict.common.unsafeNewArray
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

@Suppress("UNCHECKED_CAST")
class BinarySerializationObjectData(
    private val staticInfo: BinarySerializationStaticInfo,
    private val sections: Array<Array<out Any>>
) {
    init {
        require(staticInfo.keyResolverPairs.size == sections.size)
    }

    operator fun <T : Any> get(key: BinarySerializationStaticInfo.SectionKey<T>): Array<out T> {
        return sections[key.ordinal] as Array<out T>
    }

    fun byteSize() = with(BinarySize) {
        val resolverPairs = staticInfo.keyResolverPairs

        int64 + resolverPairs.sumOf {
            it.resolver.latestAny().getByteSize(it)
        } + resolverPairs.size * int32
    }

    fun writeTo(writer: PrimitiveValueWriter, progressReporter: ProgressReporter? = null) {
        val resolverPairs = staticInfo.keyResolverPairs
        val size = resolverPairs.size

        writer.int64(MAGIC_WORD)

        trackLoopProgressWithSubReporters(progressReporter, size) { i, subReporter ->
            val array = sections[i]
            val resolver = resolverPairs[i].resolver
            val latestSerializer = resolver.latestAny()

            writer.int32(resolver.latestVersion)
            writer.array(array, latestSerializer, subReporter)
        }
    }

    @Suppress("NOTHING_TO_INLINE")
    private inline fun <T : Any> BinarySerializerResolver<T>.latestAny() = latest as BinarySerializer<Any>

    companion object {
        private const val MAGIC_WORD = 0x00FF00FF_abcdedf00L

        fun read(
            reader: PrimitiveValueReader,
            staticInfo: BinarySerializationStaticInfo,
            progressReporter: ProgressReporter? = null
        ): BinarySerializationObjectData {
            val resolverPairs = staticInfo.keyResolverPairs
            val size = resolverPairs.size

            val magicWord = reader.int64()

            if (magicWord != MAGIC_WORD) {
                throw BinaryDataIntegrityException("Magic word is not as expected")
            }

            val arrays = unsafeNewArray<Array<out Any>>(size)

            trackLoopProgressWithSubReporters(progressReporter, size) { i, subReporter ->
                val version = reader.int32()
                val serializer = resolverPairs[i].resolver.getOrLatest(version)
                val array = reader.array(serializer, subReporter)

                arrays[i] = array
            }

            return BinarySerializationObjectData(staticInfo, arrays)
        }
    }
}

@JvmInline
value class BinarySerializationObjectDataBuilder(private val sections: Array<Array<out Any>>) {
    fun <T : Any> put(key: BinarySerializationStaticInfo.SectionKey<T>, values: Array<out T>) {
        sections[key.ordinal] = values
    }
}

inline fun BinarySerializationObjectData(
    staticInfo: BinarySerializationStaticInfo,
    block: BinarySerializationObjectDataBuilder.() -> Unit
): BinarySerializationObjectData {
    contract {
        callsInPlace(block, InvocationKind.EXACTLY_ONCE)
    }

    val sections = unsafeNewArray<Array<out Any>>(staticInfo.sectionInfo.count)
    BinarySerializationObjectDataBuilder(sections).block()

    return BinarySerializationObjectData(staticInfo, sections)
}