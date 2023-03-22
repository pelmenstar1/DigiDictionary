package io.github.pelmenstar1.digiDict.common.binarySerialization

import io.github.pelmenstar1.digiDict.common.ProgressReporter
import io.github.pelmenstar1.digiDict.common.trackLoopProgressWithSubReporters
import io.github.pelmenstar1.digiDict.common.unsafeNewArray

class BinarySerializationDecoder<TKeys : BinarySerializationSectionKeys<TKeys>> {
    private interface InternalFormatDecoder<TKeys : BinarySerializationSectionKeys<TKeys>> {
        fun decode(
            reader: PrimitiveValueReader,
            staticInfo: BinarySerializationStaticInfo<TKeys>,
            progressReporter: ProgressReporter?
        ): BinarySerializationObjectData<TKeys>
    }

    private class Version1Decoder<TKeys : BinarySerializationSectionKeys<TKeys>> : InternalFormatDecoder<TKeys> {
        override fun decode(
            reader: PrimitiveValueReader,
            staticInfo: BinarySerializationStaticInfo<TKeys>,
            progressReporter: ProgressReporter?
        ): BinarySerializationObjectData<TKeys> {
            val rawCompatInfo = reader.consumeLong()
            val compatInfo = BinarySerializationCompatInfo(rawCompatInfo)
            val arrays = decodeDataArrays(reader, staticInfo, compatInfo, progressReporter)

            return BinarySerializationObjectData(staticInfo, compatInfo, arrays)
        }
    }

    fun decode(
        reader: PrimitiveValueReader,
        staticInfo: BinarySerializationStaticInfo<TKeys>,
        progressReporter: ProgressReporter? = null
    ): BinarySerializationObjectData<TKeys> {
        return when (reader.consumeLong() /* = magic word */) {
            BinarySerializationConstants.MAGIC_WORD_1 -> decodeOld(reader, staticInfo, progressReporter)
            BinarySerializationConstants.MAGIC_WORD_2 -> decodeVersioned(reader, staticInfo, progressReporter)
            else -> throw BinarySerializationException(BinarySerializationException.REASON_UNKNOWN_VERSION)
        }
    }

    private fun decodeOld(
        reader: PrimitiveValueReader,
        staticInfo: BinarySerializationStaticInfo<TKeys>,
        progressReporter: ProgressReporter? = null
    ): BinarySerializationObjectData<TKeys> {
        // Old version of format doesn't support saving compat info, so use empty one.
        val compatInfo = BinarySerializationCompatInfo.empty()
        val arrays = decodeDataArrays(reader, staticInfo, compatInfo, progressReporter)

        return BinarySerializationObjectData(staticInfo, compatInfo, arrays)
    }

    private fun decodeVersioned(
        reader: PrimitiveValueReader,
        staticInfo: BinarySerializationStaticInfo<TKeys>,
        progressReporter: ProgressReporter? = null
    ): BinarySerializationObjectData<TKeys> {
        val internalDecoder: InternalFormatDecoder<TKeys> = when (reader.consumeInt() /* = version */) {
            1 -> Version1Decoder()
            else -> throw BinarySerializationException(BinarySerializationException.REASON_UNKNOWN_VERSION)
        }

        return internalDecoder.decode(reader, staticInfo, progressReporter)
    }

    companion object {
        private fun decodeDataArrays(
            reader: PrimitiveValueReader,
            staticInfo: BinarySerializationStaticInfo<*>,
            compatInfo: BinarySerializationCompatInfo,
            progressReporter: ProgressReporter?
        ): Array<Array<out Any>> {
            val resolvers = staticInfo.resolvers
            val keys = staticInfo.keys
            val size = resolvers.size

            val arrays = unsafeNewArray<Array<out Any>>(size)
            trackLoopProgressWithSubReporters(progressReporter, size) { i, subReporter ->
                val version = reader.consumeInt()

                val resolver = resolvers[i]
                val serializer = resolver.get(version)

                if (serializer == null) {
                    val latestKnownVersion = resolver.latestVersion

                    throw BinarySerializationException(
                        BinarySerializationException.REASON_UNKNOWN_VERSION,
                        "Latest known version for a section (${keys[i].name}) is $latestKnownVersion while requested one is $version"
                    )
                }

                arrays[i] = reader.consumeArray(serializer, compatInfo, subReporter)
            }

            return arrays
        }
    }
}