package io.github.pelmenstar1.digiDict.backup.importing

import io.github.pelmenstar1.digiDict.backup.BackupData
import io.github.pelmenstar1.digiDict.backup.BinarySerializing
import io.github.pelmenstar1.digiDict.common.ProgressReporter
import io.github.pelmenstar1.digiDict.common.binarySerialization.BinarySerializationException
import io.github.pelmenstar1.digiDict.common.binarySerialization.readSerializationObjectData
import java.io.InputStream

class BinaryDataImporter : DataImporter {
    override fun import(
        input: InputStream,
        options: ImportOptions,
        progressReporter: ProgressReporter?
    ): BackupData {
        try {
            val objectData = input.readSerializationObjectData(
                BinarySerializing.staticInfo,
                progressReporter,
                bufferSize = 4096
            )

            val records = objectData.get { records }
            val badges = objectData.get { badges }
            val badgeToMultipleRecordEntries = objectData.get { badgeToMultipleRecordEntries }

            return BackupData(records, badges, badgeToMultipleRecordEntries)
        } catch (e: BinarySerializationException) {
            val reason = when (e.reason) {
                BinarySerializationException.REASON_DATA_VALIDATION -> ImportException.REASON_DATA_VALIDATION
                BinarySerializationException.REASON_UNKNOWN_VERSION -> ImportException.REASON_UNKNOWN_VERSION
                else -> ImportException.REASON_INTERNAL
            }

            throw ImportException(reason, e.message, e)
        } catch (e: Exception) {
            throw ImportException(ImportException.REASON_INTERNAL, cause = e)
        }
    }
}