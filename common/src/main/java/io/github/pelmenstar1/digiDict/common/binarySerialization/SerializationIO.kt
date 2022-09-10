package io.github.pelmenstar1.digiDict.common.binarySerialization

import io.github.pelmenstar1.digiDict.common.ProgressReporter
import io.github.pelmenstar1.digiDict.common.trackProgressWith
import java.io.InputStream
import java.io.OutputStream

fun <TKeys : BinarySerializationSectionKeys> OutputStream.writeSerializationObjectData(
    data: BinarySerializationObjectData<TKeys>,
    progressReporter: ProgressReporter? = null,
    bufferSize: Int
) {
    val writer = PrimitiveValueWriter(this, bufferSize)
    val encoder = BinarySerializationEncoder<TKeys>()

    trackProgressWith(progressReporter) {
        encoder.encode(data, writer, progressReporter)
        writer.flush()
    }
}

fun <TKeys : BinarySerializationSectionKeys> InputStream.readSerializationObjectData(
    staticInfo: BinarySerializationStaticInfo<TKeys>,
    progressReporter: ProgressReporter?,
    bufferSize: Int
): BinarySerializationObjectData<TKeys> {
    return trackProgressWith(progressReporter) {
        val reader = PrimitiveValueReader(this, bufferSize)
        val decoder = BinarySerializationDecoder<TKeys>()

        decoder.decode(reader, staticInfo, progressReporter)
    }
}