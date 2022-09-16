package io.github.pelmenstar1.digiDict.backup.importing

import io.github.pelmenstar1.digiDict.backup.BackupData
import io.github.pelmenstar1.digiDict.common.ProgressReporter
import java.io.InputStream

interface DataImporter {
    fun import(input: InputStream, options: ImportOptions, progressReporter: ProgressReporter?): BackupData
}