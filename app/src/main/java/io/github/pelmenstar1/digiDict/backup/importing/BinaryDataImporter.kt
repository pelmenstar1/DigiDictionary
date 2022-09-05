package io.github.pelmenstar1.digiDict.backup.importing

import io.github.pelmenstar1.digiDict.backup.BackupData
import io.github.pelmenstar1.digiDict.backup.BinarySerializing
import io.github.pelmenstar1.digiDict.common.ProgressReporter
import io.github.pelmenstar1.digiDict.common.binarySerialization.readSerializationObjectDataBuffered
import java.io.InputStream

class BinaryDataImporter : DataImporter {
    override fun import(
        input: InputStream,
        options: ImportOptions,
        progressReporter: ProgressReporter?
    ): BackupData {
        val objectData = input.readSerializationObjectDataBuffered(BinarySerializing.staticInfo, progressReporter)
        val records = objectData[BinarySerializing.Sections.records]

        return BackupData(records)
    }
}