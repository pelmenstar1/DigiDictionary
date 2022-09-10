package io.github.pelmenstar1.digiDict.common.binarySerialization

import io.github.pelmenstar1.digiDict.common.ProgressReporter
import io.github.pelmenstar1.digiDict.common.trackLoopProgressWithSubReporters
import io.github.pelmenstar1.digiDict.common.unsafeNewArray

class BinarySerializationDecoder<TKeys : BinarySerializationSectionKeys> {
    fun decode(
        reader: PrimitiveValueReader,
        staticInfo: BinarySerializationStaticInfo<TKeys>,
        progressReporter: ProgressReporter? = null
    ): BinarySerializationObjectData<TKeys> {
        val resolvers = staticInfo.resolvers
        val size = resolvers.size

        val magicWord = reader.consumeLong()

        if (magicWord != BinarySerializationConstants.MAGIC_WORD) {
            throw BinaryDataIntegrityException("Magic word is not as expected")
        }

        val arrays = unsafeNewArray<Array<out Any>>(size)

        trackLoopProgressWithSubReporters(progressReporter, size) { i, subReporter ->
            val version = reader.consumeInt()
            val serializer = resolvers[i].getOrLatest(version)
            val array = reader.consumeArray(serializer, subReporter)

            arrays[i] = array
        }

        return BinarySerializationObjectData(staticInfo, arrays)
    }
}