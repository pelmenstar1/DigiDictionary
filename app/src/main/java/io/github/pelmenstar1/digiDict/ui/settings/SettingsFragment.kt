package io.github.pelmenstar1.digiDict.ui.settings

import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import io.github.pelmenstar1.digiDict.R
import io.github.pelmenstar1.digiDict.common.DataLoadState
import io.github.pelmenstar1.digiDict.common.android.showLifecycleAwareSnackbar
import io.github.pelmenstar1.digiDict.common.android.showSnackbarEventHandlerOnError
import io.github.pelmenstar1.digiDict.common.launchFlowCollector
import io.github.pelmenstar1.digiDict.common.preferences.AppPreferences
import io.github.pelmenstar1.digiDict.common.ui.SimpleProgressIndicatorDialogManager
import io.github.pelmenstar1.digiDict.common.ui.selectionDialogs.SingleSelectionDialogFragment
import io.github.pelmenstar1.digiDict.common.ui.settings.SettingsController
import io.github.pelmenstar1.digiDict.common.ui.settings.SettingsInflater
import io.github.pelmenstar1.digiDict.common.ui.settings.settingsDescriptor
import io.github.pelmenstar1.digiDict.common.ui.showAlertDialog
import io.github.pelmenstar1.digiDict.databinding.FragmentSettingsBinding
import io.github.pelmenstar1.digiDict.prefs.DigiDictAppPreferences
import io.github.pelmenstar1.digiDict.widgets.ListAppWidget
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.transform

@AndroidEntryPoint
class SettingsFragment : Fragment() {
    internal val viewModel by viewModels<SettingsViewModel>()

    private val progressIndicatorDialogManager = SimpleProgressIndicatorDialogManager()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val vm = viewModel
        val context = requireContext()

        val binding = FragmentSettingsBinding.inflate(inflater, container, false)
        val contentContainer = binding.settingsContentContainer

        val settingsInflater = SettingsInflater<DigiDictAppPreferences.Entries>(context)
        val settingsController = settingsInflater.inflate(descriptor, contentContainer).apply {
            onValueChangedHandler = viewModel::changePreferenceValue
            navController = findNavController()
            childFragmentManager = this@SettingsFragment.childFragmentManager

            bindActionHandler(ACTION_DELETE_ALL_RECORDS) {
                requestDeleteAllRecords()
            }

            // On lower API levels, there are no break strategy and hyphenation items
            if (Build.VERSION.SDK_INT >= 23) {
                bindTextFormatter({ recordTextBreakStrategy }, ResourcesBreakStrategyStringFormatter(context))
                bindTextFormatter({ recordTextHyphenationFrequency }, ResourcesHyphenationStringFormatter(context))

                registerChangeValueDialog { recordTextBreakStrategy }
                registerChangeValueDialog { recordTextHyphenationFrequency }
            }

            registerChangeValueDialog { scorePointsPerCorrectAnswer }
            registerChangeValueDialog { scorePointsPerWrongAnswer }
            registerChangeValueDialog { widgetListMaxSize }

            initDialogsIfShown()
        }

        showSnackbarEventHandlerOnError(vm.deleteAllRecordsAction, container, R.string.dbError)

        lifecycleScope.also { ls ->
            ls.launchFlowCollector(vm.deleteAllRecordsAction.successFlow) {
                if (container != null) {
                    Snackbar.make(container, R.string.settings_deleteAllSuccess, Snackbar.LENGTH_LONG)
                        .showLifecycleAwareSnackbar(lifecycle)
                }
            }

            ls.launchFlowCollector(
                vm.dataStateFlow.transform {
                    if (it is DataLoadState.Success<DigiDictAppPreferences.Snapshot>) {
                        emit(it.value.widgetListMaxSize)
                    }
                }.distinctUntilChanged()
            ) {
                ListAppWidget.updater(context).updateAllWidgets()
            }

            binding.settingsContainer.setupLoadStateFlow(ls, vm) { snapshot ->
                settingsController.applySnapshot(snapshot)
            }
        }

        progressIndicatorDialogManager.init(this, vm.operationProgressFlow)

        return binding.root
    }

    private fun requestDeleteAllRecords() {
        showAlertDialog(messageId = R.string.settings_deleteAllRecordsDialogMessage) {
            viewModel.deleteAllRecordsAction.run()
            progressIndicatorDialogManager.showDialog()
        }
    }

    private inline fun <TValue : Any> SettingsController<DigiDictAppPreferences.Entries>.registerChangeValueDialog(
        entry: DigiDictPrefsGetEntry<TValue>
    ) {
        registerChangeValueDialog(DigiDictAppPreferences.Entries.entry())
    }

    private fun <TValue : Any> SettingsController<DigiDictAppPreferences.Entries>.registerChangeValueDialog(
        entry: AppPreferences.Entry<TValue, DigiDictAppPreferences.Entries>
    ) {
        registerDialogForEntry<TValue, SingleSelectionDialogFragment<TValue>>(entry) { dialog ->
            dialog.onValueSelected = { newValue ->
                viewModel.changePreferenceValue(entry, newValue)
            }
        }
    }

    companion object {
        private const val ACTION_DELETE_ALL_RECORDS = 0

        private val descriptor = settingsDescriptor {
            groups {
                group(R.string.settings_generalGroup) {
                    linkItem(
                        nameRes = R.string.settings_linkToManageRecordBadges,
                        directions = SettingsFragmentDirections.actionSettingsToManageRecordBadges()
                    )
                }

                group(R.string.quiz) {
                    item(
                        nameRes = R.string.settings_scorePointsPerCorrectAnswer,
                        iconRes = R.drawable.ic_points_per_correct_answer,
                        preferenceEntry = { scorePointsPerCorrectAnswer },
                        clickable = true,
                    ) {
                        text()
                    }

                    item(
                        nameRes = R.string.settings_scorePointsPerWrongAnswer,
                        iconRes = R.drawable.ic_points_per_wrong_answer,
                        preferenceEntry = { scorePointsPerWrongAnswer },
                        clickable = true,
                    ) {
                        text()
                    }
                }

                group(R.string.remoteDictProviderShort) {
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

                group(R.string.settings_widgetTitle) {
                    item(
                        nameRes = R.string.settings_widgetListMaxSize,
                        iconRes = R.drawable.ic_widget_list_max_size,
                        preferenceEntry = { widgetListMaxSize },
                        clickable = true
                    ) {
                        text()
                    }
                }

                group(R.string.settings_backupTitle) {
                    linkItem(
                        R.string.settings_export,
                        directions = SettingsFragmentDirections.actionSettingsToExportConfiguration()
                    )
                    linkItem(
                        R.string.settings_import,
                        directions = SettingsFragmentDirections.actionSettingsToImportConfiguration()
                    )
                }

                group(R.string.settings_miscGroup) {
                    actionItem(ACTION_DELETE_ALL_RECORDS, R.string.settings_deleteAllRecords)

                    if (Build.VERSION.SDK_INT >= 23) {
                        item(
                            nameRes = R.string.settings_breakStrategy,
                            preferenceEntry = { recordTextBreakStrategy },
                            clickable = true
                        ) {
                            text()
                        }

                        item(
                            nameRes = R.string.settings_hyphenation,
                            preferenceEntry = { recordTextHyphenationFrequency },
                            clickable = true
                        ) {
                            text()
                        }
                    }
                }
            }

            dialogs {
                if (Build.VERSION.SDK_INT >= 23) {
                    dialog<_, BreakStrategyDialogFragment>(
                        entry = { recordTextBreakStrategy },
                        createArgs = BreakStrategyDialogFragment::createArguments
                    )
                    dialog<_, HyphenationDialogFragment>(
                        entry = { recordTextHyphenationFrequency },
                        createArgs = HyphenationDialogFragment::createArguments
                    )
                }

                numberDialog(
                    entry = { scorePointsPerCorrectAnswer },
                    titleRes = R.string.settings_selectValueDialogTitle,
                    start = 1,
                    endInclusive = 10
                )

                numberDialog(
                    entry = { scorePointsPerWrongAnswer },
                    titleRes = R.string.settings_selectValueDialogTitle,
                    start = 1,
                    endInclusive = 10
                )

                numberDialog(
                    entry = { widgetListMaxSize },
                    titleRes = R.string.settings_selectValueDialogTitle,
                    start = 10,
                    endInclusive = 40,
                    step = 5
                )
            }
        }
    }
}