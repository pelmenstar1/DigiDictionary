package io.github.pelmenstar1.digiDict.common.binarySerialization

import io.github.pelmenstar1.digiDict.common.ProgressReporter
import io.github.pelmenstar1.digiDict.common.trackProgressWith
import java.io.InputStream
import java.io.OutputStream

fun OutputStream.writeSerializationObjectData(
    data: BinarySerializationObjectData,
    progressReporter: ProgressReporter? = null,
    bufferSize: Int
) {
    val writer = PrimitiveValueWriter(this, bufferSize)

    trackProgressWith(progressReporter) {
        data.writeTo(writer, progressReporter)
        writer.flush()
    }
}

fun InputStream.readSerializationObjectDataBuffered(
    staticInfo: BinarySerializationStaticInfo,
    progressReporter: ProgressReporter? = null,
    bufferSize: Int = 4096
): BinarySerializationObjectData {
    return buffered(bufferSize).readSerializationObjectData(staticInfo, progressReporter)
}

fun InputStream.readSerializationObjectData(
    staticInfo: BinarySerializationStaticInfo,
    progressReporter: ProgressReporter?
): BinarySerializationObjectData {
    return trackProgressWith(progressReporter) {
        BinarySerializationObjectData.read(PrimitiveValueReader(this), staticInfo, progressReporter)
    }
}