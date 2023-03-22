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
        version: Int,
        progressReporter: ProgressReporter?
    ) {
        try {
            val binaryCompatInfo = data.compatInfo.toBinarySerializationCompatInfo()

            val objectData = BinarySerializationObjectData(BinarySerializing.staticInfo, binaryCompatInfo) {
                put(BinarySerializing.SectionKeys.records, data.records)
                put(BinarySerializing.SectionKeys.badges, data.badges)
                put(BinarySerializing.SectionKeys.badgeToMultipleRecordEntries, data.badgeToMultipleRecordEntries)
            }

            output.writeSerializationObjectData(objectData, version, progressReporter, bufferSize = 4096)
        } catch (e: Exception) {
            throw ExportException(cause = e)
        }
    }
}