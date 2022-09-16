package io.github.pelmenstar1.digiDict.backup.exporting

import io.github.pelmenstar1.digiDict.backup.BackupData
import io.github.pelmenstar1.digiDict.common.ProgressReporter
import io.github.pelmenstar1.digiDict.common.trackProgressWith
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.encodeToStream
import java.io.OutputStream

class JsonDataExporter : DataExporter {
    override fun export(
        output: OutputStream,
        data: BackupData,
        progressReporter: ProgressReporter?
    ) {
        try {
            trackProgressWith(progressReporter) {
                Json.encodeToStream(data, output)
            }
        } catch (e: Exception) {
            throw ExportException(cause = e)
        }
    }
}