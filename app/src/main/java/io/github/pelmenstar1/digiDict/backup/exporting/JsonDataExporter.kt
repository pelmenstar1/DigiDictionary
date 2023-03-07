package io.github.pelmenstar1.digiDict.backup.exporting

import io.github.pelmenstar1.digiDict.backup.BackupData
import io.github.pelmenstar1.digiDict.backup.JsonBackupData0
import io.github.pelmenstar1.digiDict.common.ProgressReporter
import io.github.pelmenstar1.digiDict.common.trackProgressWith
import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.encodeToStream
import java.io.OutputStream

class JsonDataExporter : DataExporter {
    @Suppress("UNCHECKED_CAST")
    override fun export(
        output: OutputStream,
        data: BackupData,
        version: Int,
        progressReporter: ProgressReporter?
    ) {
        try {
            val wrappedData: Any
            val serializer: KSerializer<Any>

            when (version) {
                0 -> {
                    wrappedData = data
                    serializer = BackupData.serializer() as KSerializer<Any>
                }
                1 -> {
                    wrappedData = JsonBackupData0(data.records, data.badges, data.badgeToMultipleRecordEntries)
                    serializer = JsonBackupData0.serializer() as KSerializer<Any>
                }
                else -> throw IllegalArgumentException("Invalid version ($version)")
            }

            trackProgressWith(progressReporter) {
                Json.encodeToStream(serializer, wrappedData, output)
            }
        } catch (e: Exception) {
            throw ExportException(cause = e)
        }
    }
}