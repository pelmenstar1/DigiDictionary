package io.github.pelmenstar1.digiDict.ui.settings

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dagger.hilt.android.AndroidEntryPoint
import io.github.pelmenstar1.digiDict.R
import io.github.pelmenstar1.digiDict.backup.RecordImportExportManager
import io.github.pelmenstar1.digiDict.common.*
import io.github.pelmenstar1.digiDict.common.ui.LoadingIndicatorDialog
import io.github.pelmenstar1.digiDict.databinding.FragmentSettingsBinding
import io.github.pelmenstar1.digiDict.prefs.AppPreferences
import io.github.pelmenstar1.digiDict.widgets.ListAppWidget
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.transform
import javax.inject.Inject

@AndroidEntryPoint
class SettingsFragment : Fragment() {
    internal val viewModel by viewModels<SettingsViewModel>()

    @Inject
    lateinit var messageMapper: MessageMapper<SettingsMessage>

    private var isLoadingProgressDialogShown = false

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val vm = viewModel
        val context = requireContext()

        val binding = FragmentSettingsBinding.inflate(inflater, container, false)

        RecordImportExportManager.init(this)

        val contentContainer = binding.settingsContentContainer

        SettingsInflater(context).inflate(descriptor, container = contentContainer).also {
            it.onValueChangedHandler = viewModel::changePreferenceValue

            it.bindActionHandler(ACTION_IMPORT) {
                vm.importData(context)
                showLoadingProgressDialog()
            }

            it.bindActionHandler(ACTION_EXPORT) {
                vm.exportData(context)
                showLoadingProgressDialog()
            }

            it.bindActionHandler(ACTION_DELETE_ALL_RECORDS) {
                requestDeleteAllRecords()
            }
        }

        vm.onOperationError.handler = {
            hideLoadingProgressDialog()
        }

        lifecycleScope.run {
            launchMessageFlowCollector(viewModel.messageFlow, messageMapper, container)

            binding.settingsContainer.setupLoadStateFlow(this@run, vm) { snapshot ->
                SettingsInflater.applySnapshot(snapshot, contentContainer)
            }

            launchFlowCollector(
                vm.dataStateFlow.transform {
                    if (it is DataLoadState.Success<AppPreferences.Snapshot>) {
                        emit(it.value.widgetListMaxSize)
                    }
                }.distinctUntilChanged()
            ) {
                ListAppWidget.updater(context).updateAllWidgets()
            }
        }

        if (savedInstanceState != null) {
            if (savedInstanceState.getBoolean(STATE_IS_LOADING_PROGRESS_DIALOG_SHOWN, false)) {
                showLoadingProgressDialog()
            }
        }

        return binding.root
    }

    private fun requestDeleteAllRecords() {
        MaterialAlertDialogBuilder(requireContext())
            .setMessage(R.string.settings_deleteAllRecordsDialogMessage)
            .setPositiveButton(android.R.string.ok) { _, _ ->
                viewModel.deleteAllRecords()
                showLoadingProgressDialog()
            }
            .setNegativeButton(android.R.string.cancel, NO_OP_DIALOG_ON_CLICK_LISTENER)
            .show()
    }

    private fun showLoadingProgressDialog() {
        var dialog: LoadingIndicatorDialog? = null

        lifecycleScope.launchFlowCollector(viewModel.operationProgressFlow) { progress ->
            when (progress) {
                ProgressReporter.UNREPORTED -> {
                    isLoadingProgressDialogShown = false
                }
                100 -> {
                    isLoadingProgressDialogShown = false
                    dialog?.dismissNow()

                    return@launchFlowCollector
                }
                else -> {
                    if (!isLoadingProgressDialogShown) {
                        // Hide previous loading-progress-dialog to prevent state when two dialogs are shown
                        // at the same time and only the last one is dismissed at the end.
                        hideLoadingProgressDialog()

                        isLoadingProgressDialogShown = true

                        dialog = LoadingIndicatorDialog().also {
                            it.showNow(childFragmentManager, LOADING_PROGRESS_DIALOG_TAG)
                        }
                    }

                    dialog?.setProgress(progress)
                }
            }
        }
    }

    // TODO: Do not recreate the dialog, init existing one
    private fun hideLoadingProgressDialog() {
        isLoadingProgressDialogShown = false

        val fm = childFragmentManager
        val prevDialog = fm.findFragmentByTag(LOADING_PROGRESS_DIALOG_TAG)

        prevDialog?.also {
            fm.beginTransaction().remove(it).commitNow()
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)

        outState.putBoolean(STATE_IS_LOADING_PROGRESS_DIALOG_SHOWN, isLoadingProgressDialogShown)
    }

    override fun onDestroy() {
        super.onDestroy()

        RecordImportExportManager.release()
    }

    companion object {
        private const val STATE_IS_LOADING_PROGRESS_DIALOG_SHOWN =
            "io.github.pelmenstar1.digiDict.ui.settings.SettingsFragment.isLoadingProgressDialogShown"

        private const val LOADING_PROGRESS_DIALOG_TAG = "LoadingIndicatorDialog"

        private const val ACTION_IMPORT = 0
        private const val ACTION_EXPORT = 1
        private const val ACTION_DELETE_ALL_RECORDS = 2

        private val descriptor = settingsDescriptor {
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
    }
}