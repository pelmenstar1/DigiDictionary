package io.github.pelmenstar1.digiDict.backup.exporting

import io.github.pelmenstar1.digiDict.backup.BackupData
import io.github.pelmenstar1.digiDict.backup.BinarySerializing
import io.github.pelmenstar1.digiDict.common.ProgressReporter
import io.github.pelmenstar1.digiDict.common.binarySerialization.BinarySerializationObjectData
import io.github.pelmenstar1.digiDict.common.binarySerialization.writeSerializationObjectData
import java.io.OutputStream

class BinaryDataExporter : DataExporter {
    override fun export(
        output: OutputStream,
        data: BackupData,
        options: ExportOptions,
        progressReporter: ProgressReporter?
    ) {
        val objectData = BinarySerializationObjectData(BinarySerializing.staticInfo) {
            put(BinarySerializing.Sections.records, data.records)
        }

        output.writeSerializationObjectData(objectData, progressReporter, bufferSize = 4096)
    }
}