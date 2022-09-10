package io.github.pelmenstar1.digiDict.common.binarySerialization

import io.github.pelmenstar1.digiDict.common.ProgressReporter
import io.github.pelmenstar1.digiDict.common.trackLoopProgressWithSubReporters

class BinarySerializationEncoder<TKeys : BinarySerializationSectionKeys> {
    @Suppress("UNCHECKED_CAST")
    fun encode(
        objectData: BinarySerializationObjectData<TKeys>,
        writer: PrimitiveValueWriter,
        progressReporter: ProgressReporter? = null
    ) {
        val staticInfo = objectData.staticInfo
        val resolvers = staticInfo.resolvers
        val sections = objectData.sections

        val size = sections.size

        writer.emit(BinarySerializationConstants.MAGIC_WORD)

        trackLoopProgressWithSubReporters(progressReporter, size) { i, subReporter ->
            val array = sections[i]
            val resolver = resolvers[i]
            val latestSerializer = resolver.latest as BinarySerializer<Any>

            writer.emit(resolver.latestVersion)
            writer.emit(array, latestSerializer, subReporter)
        }
    }
}