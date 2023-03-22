package io.github.pelmenstar1.digiDict.backup.importing

import io.github.pelmenstar1.digiDict.backup.BackupData
import io.github.pelmenstar1.digiDict.common.ProgressReporter
import java.io.InputStream

/**
 * Provides the means to read given backup data to output stream.
 */
interface DataImporter {
    /**
     * Reads the [BackupData] from [input] stream in specified by the implementation format.
     * If the implementation supports reporting about the progress made, [progressReporter] is used if specified.
     */
    fun import(
        input: InputStream,
        options: ImportOptions,
        progressReporter: ProgressReporter?
    ): BackupData
}