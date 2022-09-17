package io.github.pelmenstar1.digiDict.ui.exportConfig

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.pelmenstar1.digiDict.backup.BackupFormat
import io.github.pelmenstar1.digiDict.backup.BackupManager
import io.github.pelmenstar1.digiDict.backup.exporting.ExportOptions
import io.github.pelmenstar1.digiDict.common.ProgressReporter
import io.github.pelmenstar1.digiDict.common.trackProgressWith
import io.github.pelmenstar1.digiDict.common.viewModelAction
import io.github.pelmenstar1.digiDict.data.AppDatabase
import kotlinx.coroutines.Dispatchers
import javax.inject.Inject

@HiltViewModel
class ExportConfigurationViewModel @Inject constructor(
    private val appDatabase: AppDatabase
) : ViewModel() {
    private val progressReporter = ProgressReporter()
    val progressFlow = progressReporter.progressFlow

    var selectedFormat: BackupFormat? = null
    var exportBadges: Boolean = true

    val exportAction = viewModelAction<Context, Uri>(TAG, Dispatchers.IO) { context, uri ->
        val options = ExportOptions(exportBadges)
        val reporter = progressReporter

        trackProgressWith(reporter) {
            val data = BackupManager.createBackupData(
                appDatabase,
                options,
                reporter.subReporter(completed = 0, target = 50)  // Treat extracting the data as half of the job.
            )

            BackupManager.export(
                context,
                uri,
                data,
                selectedFormat!!,
                reporter.subReporter(completed = 50, target = 100)
            )
        }
    }

    companion object {
        private const val TAG = "ExportConfViewModel"
    }
}