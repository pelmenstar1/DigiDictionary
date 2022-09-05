package io.github.pelmenstar1.digiDict.backup.importing

import io.github.pelmenstar1.digiDict.backup.BackupData
import io.github.pelmenstar1.digiDict.common.ProgressReporter
import io.github.pelmenstar1.digiDict.common.trackProgressWith
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromStream
import java.io.InputStream

class JsonDataImporter : DataImporter {
    override fun import(
        input: InputStream,
        options: ImportOptions,
        progressReporter: ProgressReporter?
    ): BackupData {
        return trackProgressWith(progressReporter) {
            Json.decodeFromStream(input)
        }
    }
}