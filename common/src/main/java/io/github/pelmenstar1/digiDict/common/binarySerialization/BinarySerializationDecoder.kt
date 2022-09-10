package io.github.pelmenstar1.digiDict.common.binarySerialization

import io.github.pelmenstar1.digiDict.common.ProgressReporter
import io.github.pelmenstar1.digiDict.common.trackLoopProgressWithSubReporters
import io.github.pelmenstar1.digiDict.common.unsafeNewArray

class BinarySerializationDecoder<TKeys : BinarySerializationSectionKeys<TKeys>> {
    fun decode(
        reader: PrimitiveValueReader,
        staticInfo: BinarySerializationStaticInfo<TKeys>,
        progressReporter: ProgressReporter? = null
    ): BinarySerializationObjectData<TKeys> {
        val resolvers = staticInfo.resolvers
        val keys = staticInfo.keys
        val size = resolvers.size

        val magicWord = reader.consumeLong()
        checkDataValidity("Wrong magic word") { magicWord == BinarySerializationConstants.MAGIC_WORD }

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

            val array = reader.consumeArray(serializer, subReporter)

            arrays[i] = array
        }

        return BinarySerializationObjectData(staticInfo, arrays)
    }
}