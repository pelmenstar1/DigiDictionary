package io.github.pelmenstar1.digiDict.backup.exporting

import io.github.pelmenstar1.digiDict.backup.BackupData
import io.github.pelmenstar1.digiDict.common.ProgressReporter
import java.io.OutputStream

/**
 * Provides the means to export given backup data to output stream.
 */
interface DataExporter {
    /**
     * Writes the [data] to [output] stream in specified by the implementation format.
     * As the format can be versioned [version] is specified too. If the implementation supports reporting
     * about the progress made, [progressReporter] is used if specified.
     */
    fun export(
        output: OutputStream,
        data: BackupData,
        version: Int,
        progressReporter: ProgressReporter? = null
    )
}