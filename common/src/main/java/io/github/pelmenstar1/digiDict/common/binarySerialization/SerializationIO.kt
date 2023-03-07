package io.github.pelmenstar1.digiDict.common.binarySerialization

import io.github.pelmenstar1.digiDict.common.ProgressReporter
import io.github.pelmenstar1.digiDict.common.trackProgressWith
import java.io.InputStream
import java.io.OutputStream
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

private inline fun <R> wrapExceptions(block: () -> R): R {
    contract {
        callsInPlace(block, InvocationKind.EXACTLY_ONCE)
    }

    return try {
        block()
    } catch (e: BinarySerializationException) {
        throw e
    } catch (e: Exception) {
        throw BinarySerializationException(BinarySerializationException.REASON_INTERNAL, "Internal error", e)
    }
}

fun <TKeys : BinarySerializationSectionKeys<TKeys>> OutputStream.writeSerializationObjectData(
    data: BinarySerializationObjectData<TKeys>,
    version: Int,
    progressReporter: ProgressReporter? = null,
    bufferSize: Int
) {
    wrapExceptions {
        val writer = PrimitiveValueWriter(this, bufferSize)
        val encoder = BinarySerializationEncoder.createVersioned<TKeys>(version)

        trackProgressWith(progressReporter) {
            encoder.encode(data, writer, progressReporter)
            writer.flush()
        }
    }
}

fun <TKeys : BinarySerializationSectionKeys<TKeys>> InputStream.readSerializationObjectData(
    staticInfo: BinarySerializationStaticInfo<TKeys>,
    progressReporter: ProgressReporter?,
    bufferSize: Int
): BinarySerializationObjectData<TKeys> {
    return wrapExceptions {
        trackProgressWith(progressReporter) {
            val reader = PrimitiveValueReader(this, bufferSize)
            val decoder = BinarySerializationDecoder<TKeys>()

            decoder.decode(reader, staticInfo, progressReporter)
        }
    }
}