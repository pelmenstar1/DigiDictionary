package io.github.pelmenstar1.digiDict.ui.settings

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import dagger.hilt.android.AndroidEntryPoint
import io.github.pelmenstar1.digiDict.R
import io.github.pelmenstar1.digiDict.backup.RecordImportExportManager
import io.github.pelmenstar1.digiDict.common.*
import io.github.pelmenstar1.digiDict.common.ui.LoadingIndicatorDialog
import io.github.pelmenstar1.digiDict.common.ui.launchMessageFlowCollector
import io.github.pelmenstar1.digiDict.common.ui.showAlertDialog
import io.github.pelmenstar1.digiDict.databinding.FragmentSettingsBinding
import io.github.pelmenstar1.digiDict.prefs.AppPreferences
import io.github.pelmenstar1.digiDict.widgets.ListAppWidget
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.transform
import javax.inject.Inject

@AndroidEntryPoint
class SettingsFragment : Fragment() {
    internal val viewModel by viewModels<SettingsViewModel>()

    @Inject
    lateinit var messageMapper: MessageMapper<SettingsMessage>

    private var loadingProgressCollectionJob: Job? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val vm = viewModel
        val context = requireContext()

        val binding = FragmentSettingsBinding.inflate(inflater, container, false)
        val contentContainer = binding.settingsContentContainer

        RecordImportExportManager.init(this)

        SettingsInflater(context).inflate(descriptor, container = contentContainer).run {
            onValueChangedHandler = viewModel::changePreferenceValue
            navController = findNavController()

            bindActionHandler(ACTION_IMPORT) {
                invokeWithLoadingIndicator { importData(context) }
            }

            bindActionHandler(ACTION_EXPORT) {
                invokeWithLoadingIndicator { exportData(context) }
            }

            bindActionHandler(ACTION_DELETE_ALL_RECORDS) {
                requestDeleteAllRecords()
            }
        }

        lifecycleScope.also { ls ->
            launchMessageFlowCollector(vm.messageFlow, messageMapper, container)

            ls.launchFlowCollector(vm.operationErrorFlow.filterNotNull()) {
                hideLoadingProgressDialog()

                // Cancel the job, it's no longer needed.
                loadingProgressCollectionJob?.cancel()
            }

            ls.launchFlowCollector(
                vm.dataStateFlow.transform {
                    if (it is DataLoadState.Success<AppPreferences.Snapshot>) {
                        emit(it.value.widgetListMaxSize)
                    }
                }.distinctUntilChanged()
            ) {
                ListAppWidget.updater(context).updateAllWidgets()
            }

            binding.settingsContainer.setupLoadStateFlow(ls, vm) { snapshot ->
                SettingsInflater.applySnapshot(snapshot, contentContainer)
            }
        }

        findLoadingIndicatorDialog(childFragmentManager)?.also {
            showOrBindLoadingIndicatorDialog()
        }

        return binding.root
    }

    private fun requestDeleteAllRecords() {
        showAlertDialog(messageId = R.string.settings_deleteAllRecordsDialogMessage) {
            invokeWithLoadingIndicator { deleteAllRecords() }
        }
    }

    private inline fun invokeWithLoadingIndicator(action: SettingsViewModel.() -> Unit) {
        viewModel.action()
        showOrBindLoadingIndicatorDialog()
    }

    private fun showOrBindLoadingIndicatorDialog() {
        loadingProgressCollectionJob?.let {
            if (it.isActive) {
                debugLog(LOG_TAG) {
                    info("loadingProgressCollectionJob is already started")
                }

                it.cancel()
            }
        }
        var dialog: LoadingIndicatorDialog? = null

        loadingProgressCollectionJob = lifecycleScope.launchFlowCollector(
            viewModel.operationProgressFlow.cancelAfter { it == 100 }
        ) { progress ->
            if (progress == 100) {
                dialog?.dismissNow()

                // To be sure dialog won't be reused after it's dismissed.
                dialog = null
            } else if (progress != ProgressReporter.UNREPORTED) {
                var tempDialog = dialog
                if (tempDialog == null) {
                    val fm = childFragmentManager

                    // Try to find existing dialog to re-use it.
                    tempDialog = findLoadingIndicatorDialog(fm)

                    if (tempDialog == null) {
                        // If there's no loading-indicator-dialog, show it.
                        tempDialog = LoadingIndicatorDialog().also {
                            it.showNow(fm, LOADING_PROGRESS_DIALOG_TAG)
                        }
                    }

                    dialog = tempDialog
                }

                tempDialog.setProgress(progress)
            }
        }.also {
            it.invokeOnCompletion { loadingProgressCollectionJob = null }
        }
    }

    private fun hideLoadingProgressDialog() {
        findLoadingIndicatorDialog(childFragmentManager)?.dismissNow()
    }

    override fun onDestroy() {
        super.onDestroy()

        RecordImportExportManager.release()
    }

    companion object {
        private const val LOG_TAG = "SettingsFragment"
        private const val LOADING_PROGRESS_DIALOG_TAG = "LoadingIndicatorDialog"

        private const val ACTION_IMPORT = 0
        private const val ACTION_EXPORT = 1
        private const val ACTION_DELETE_ALL_RECORDS = 2

        private val descriptor = settingsDescriptor {
            itemGroup(R.string.settings_generalGroup) {
                linkItem(
                    nameRes = R.string.settings_linkToManageRecordBadges,
                    directions = SettingsFragmentDirections.actionSettingsToManageRecordBadges()
                )
            }

            itemGroup(R.string.quiz) {
                item(
                    nameRes = R.string.settings_scorePointsPerCorrectAnswer,
                    iconRes = R.drawable.ic_points_per_correct_answer,
                    preferenceEntry = { scorePointsPerCorrectAnswer }
                ) {
                    rangeSpinner(start = 1, endInclusive = 10)
                }

                item(
                    nameRes = R.string.settings_scorePointsPerWrongAnswer,
                    iconRes = R.drawable.ic_points_per_wrong_answer,
                    preferenceEntry = { scorePointsPerWrongAnswer }
                ) {
                    rangeSpinner(start = 1, endInclusive = 10)
                }
            }

            itemGroup(R.string.remoteDictProviderShort) {
                item(
                    nameRes = R.string.settings_openBrowserInApp,
                    iconRes = R.drawable.ic_open_browser_in_app,
                    preferenceEntry = { useCustomTabs }
                ) {
                    switch()
                }

                linkItem(
                    nameRes = R.string.settings_linkToManageRemoteDictProviders,
                    directions = SettingsFragmentDirections.actionSettingsToManageRemoteDictionaryProviders()
                )
            }

            itemGroup(R.string.remindRecords_label) {
                item(
                    nameRes = R.string.settings_remindMaxItems,
                    iconRes = R.drawable.ic_list_numbered,
                    preferenceEntry = { remindItemsSize },
                ) {
                    rangeSpinner(
                        start = 10,
                        endInclusive = 30,
                        step = 5
                    )
                }

                item(
                    nameRes = R.string.settings_remindShowMeaning,
                    iconRes = R.drawable.ic_remind_show_meaning,
                    preferenceEntry = { remindShowMeaning }
                ) {
                    switch()
                }
            }

            itemGroup(R.string.settings_widgetTitle) {
                item(
                    nameRes = R.string.settings_widgetListMaxSize,
                    iconRes = R.drawable.ic_widget_list_max_size,
                    preferenceEntry = { widgetListMaxSize }
                ) {
                    rangeSpinner(
                        start = 10,
                        endInclusive = 40,
                        step = 5
                    )
                }
            }

            actionGroup(R.string.settings_backupTitle) {
                action(ACTION_EXPORT, R.string.settings_export)
                action(ACTION_IMPORT, R.string.settings_import)
                action(ACTION_DELETE_ALL_RECORDS, R.string.settings_deleteAllRecords)
            }
        }

        internal fun findLoadingIndicatorDialog(fm: FragmentManager): LoadingIndicatorDialog? {
            return fm.findFragmentByTag(LOADING_PROGRESS_DIALOG_TAG) as LoadingIndicatorDialog?
        }
    }
}