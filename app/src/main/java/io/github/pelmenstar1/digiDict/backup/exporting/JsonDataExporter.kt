package io.github.pelmenstar1.digiDict.backup.exporting

import io.github.pelmenstar1.digiDict.backup.BackupData
import io.github.pelmenstar1.digiDict.backup.JsonBackupData0
import io.github.pelmenstar1.digiDict.common.ProgressReporter
import io.github.pelmenstar1.digiDict.common.trackProgressWith
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.encodeToStream
import java.io.OutputStream

class JsonDataExporter : DataExporter {
    override fun export(
        output: OutputStream,
        data: BackupData,
        version: Int,
        progressReporter: ProgressReporter?
    ) {
        require(version == 0 || version == 1) { "Invalid version ($version)" }

        try {
            trackProgressWith(progressReporter) {
                if (version == 0) {
                    Json.encodeToStream(
                        JsonBackupData0(data.records, data.badges, data.badgeToMultipleRecordEntries),
                        output
                    )
                } else {
                    Json.encodeToStream(data, output)
                }

                output.flush()
            }
        } catch (e: Exception) {
            throw ExportException("Failed to export the data as JSON (version=$version)", e)
        }
    }
}
