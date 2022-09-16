package io.github.pelmenstar1.digiDict.ui.settings

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import dagger.hilt.android.AndroidEntryPoint
import io.github.pelmenstar1.digiDict.R
import io.github.pelmenstar1.digiDict.common.DataLoadState
import io.github.pelmenstar1.digiDict.common.MessageMapper
import io.github.pelmenstar1.digiDict.common.launchFlowCollector
import io.github.pelmenstar1.digiDict.common.ui.ProgressIndicatorDialogManager
import io.github.pelmenstar1.digiDict.common.ui.launchMessageFlowCollector
import io.github.pelmenstar1.digiDict.common.ui.showAlertDialog
import io.github.pelmenstar1.digiDict.databinding.FragmentSettingsBinding
import io.github.pelmenstar1.digiDict.prefs.AppPreferences
import io.github.pelmenstar1.digiDict.widgets.ListAppWidget
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.transform
import javax.inject.Inject

@AndroidEntryPoint
class SettingsFragment : Fragment() {
    internal val viewModel by viewModels<SettingsViewModel>()

    @Inject
    lateinit var messageMapper: MessageMapper<SettingsMessage>

    private val progressIndicatorDialogManager = ProgressIndicatorDialogManager()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val vm = viewModel
        val context = requireContext()

        val binding = FragmentSettingsBinding.inflate(inflater, container, false)
        val contentContainer = binding.settingsContentContainer

        SettingsInflater(context).inflate(descriptor, container = contentContainer).run {
            onValueChangedHandler = viewModel::changePreferenceValue
            val nc = findNavController().also { navController = it }

            bindActionHandler(ACTION_IMPORT) {
                nc.navigate(SettingsFragmentDirections.actionSettingsToImportConfiguration())
            }

            bindActionHandler(ACTION_EXPORT) {
                nc.navigate(SettingsFragmentDirections.actionSettingsToExportConfiguration())
            }

            bindActionHandler(ACTION_DELETE_ALL_RECORDS) {
                requestDeleteAllRecords()
            }
        }

        lifecycleScope.also { ls ->
            launchMessageFlowCollector(vm.messageFlow, messageMapper, container)

            ls.launchFlowCollector(vm.operationErrorFlow.filterNotNull()) {
                // Dismiss the dialog and cancel the job, they are no longer needed after an error.
                progressIndicatorDialogManager.cancel()
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

        progressIndicatorDialogManager.init(this, vm.operationProgressFlow)

        return binding.root
    }

    private fun requestDeleteAllRecords() {
        showAlertDialog(messageId = R.string.settings_deleteAllRecordsDialogMessage) {
            invokeWithLoadingIndicator { deleteAllRecords() }
        }
    }

    private inline fun invokeWithLoadingIndicator(action: SettingsViewModel.() -> Unit) {
        viewModel.action()
        progressIndicatorDialogManager.showDialog()
    }

    companion object {
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
    }
}