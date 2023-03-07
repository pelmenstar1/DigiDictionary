package io.github.pelmenstar1.digiDict.common.binarySerialization

import io.github.pelmenstar1.digiDict.common.ProgressReporter
import io.github.pelmenstar1.digiDict.common.trackLoopProgressWithSubReporters

abstract class BinarySerializationEncoder<TKeys : BinarySerializationSectionKeys<TKeys>> {
    private class Impl0<TKeys : BinarySerializationSectionKeys<TKeys>> : BinarySerializationEncoder<TKeys>() {
        override fun encode(
            objectData: BinarySerializationObjectData<TKeys>,
            writer: PrimitiveValueWriter,
            progressReporter: ProgressReporter?
        ) {
            val staticInfo = objectData.staticInfo

            writer.emit(BinarySerializationConstants.MAGIC_WORD_1)

            emitSections(writer, objectData, staticInfo, progressReporter)
        }
    }

    private class Impl1<TKeys : BinarySerializationSectionKeys<TKeys>> : BinarySerializationEncoder<TKeys>() {
        override fun encode(
            objectData: BinarySerializationObjectData<TKeys>,
            writer: PrimitiveValueWriter,
            progressReporter: ProgressReporter?
        ) {
            val staticInfo = objectData.staticInfo

            writer.emit(BinarySerializationConstants.MAGIC_WORD_2)
            writer.emit(BinarySerializationConstants.LATEST_INTERNAL_FORMAT_VERSION)
            writer.emit(objectData.compatInfo.bits)

            emitSections(writer, objectData, staticInfo, progressReporter)
        }
    }

    abstract fun encode(
        objectData: BinarySerializationObjectData<TKeys>,
        writer: PrimitiveValueWriter,
        progressReporter: ProgressReporter? = null
    )

    companion object {
        fun <TKeys : BinarySerializationSectionKeys<TKeys>> createVersioned(version: Int): BinarySerializationEncoder<TKeys> {
            return when (version) {
                0 -> Impl0()
                1 -> Impl1()
                else -> throw IllegalArgumentException("Invalid version ($version)")
            }
        }

        @Suppress("UNCHECKED_CAST")
        private fun emitSections(
            writer: PrimitiveValueWriter,
            objectData: BinarySerializationObjectData<*>,
            staticInfo: BinarySerializationStaticInfo<*>,
            progressReporter: ProgressReporter?
        ) {
            val resolvers = staticInfo.resolvers
            val sections = objectData.sections
            val compatInfo = objectData.compatInfo
            val size = sections.size

            trackLoopProgressWithSubReporters(progressReporter, size) { i, subReporter ->
                val array = sections[i]
                val resolver = resolvers[i]
                val latestSerializer = resolver.latest as BinarySerializer<Any>

                writer.emit(resolver.latestVersion)
                writer.emit(array, latestSerializer, compatInfo, subReporter)
            }
        }
    }
}