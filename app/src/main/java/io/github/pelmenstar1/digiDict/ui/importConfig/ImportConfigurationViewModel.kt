package io.github.pelmenstar1.digiDict.ui.importConfig

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.pelmenstar1.digiDict.backup.BackupFormat
import io.github.pelmenstar1.digiDict.backup.BackupManager
import io.github.pelmenstar1.digiDict.backup.importing.ImportOptions
import io.github.pelmenstar1.digiDict.common.ProgressReporter
import io.github.pelmenstar1.digiDict.common.android.viewModelAction
import io.github.pelmenstar1.digiDict.common.trackProgressWith
import io.github.pelmenstar1.digiDict.data.AppDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import javax.inject.Inject

@HiltViewModel
class ImportConfigurationViewModel @Inject constructor(
    private val appDatabase: AppDatabase
) : ViewModel() {
    private val importBadgesFlow = MutableStateFlow(true)
    private val operationProgressReporter = ProgressReporter()

    val isReplaceBadgesEnabled = importBadgesFlow
    val operationProgressFlow = operationProgressReporter.progressFlow

    var importBadges: Boolean
        get() = importBadgesFlow.value
        set(value) {
            importBadgesFlow.value = value
        }

    var replaceBadges: Boolean = true
    val importAction = viewModelAction<Context, Uri, BackupFormat>(TAG, Dispatchers.IO) { context, source, format ->
        val options = ImportOptions(importBadges, replaceBadges)
        val reporter = operationProgressReporter

        trackProgressWith(reporter) {
            val data = BackupManager.import(
                context,
                source, format, options,
                reporter.subReporter(completed = 0, target = 50) // Treat reading the data as half of the job.
            )

            BackupManager.deployImportData(
                data,
                options,
                appDatabase,
                reporter.subReporter(completed = 50, target = 100)
            )
        }
    }

    companion object {
        private const val TAG = "ImportConfViewModel"
    }
}