package io.github.pelmenstar1.digiDict.backup.exporting

import io.github.pelmenstar1.digiDict.backup.BackupData
import io.github.pelmenstar1.digiDict.common.ProgressReporter
import java.io.OutputStream

interface DataExporter {
    fun export(
        output: OutputStream,
        data: BackupData,
        progressReporter: ProgressReporter? = null
    )
}