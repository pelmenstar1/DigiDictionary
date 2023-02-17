package io.github.pelmenstar1.digiDict.ui.settings

import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.RequiresApi
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
import io.github.pelmenstar1.digiDict.common.firstSuccess
import io.github.pelmenstar1.digiDict.common.launchFlowCollector
import io.github.pelmenstar1.digiDict.common.preferences.AppPreferences
import io.github.pelmenstar1.digiDict.common.ui.SimpleProgressIndicatorDialogManager
import io.github.pelmenstar1.digiDict.common.ui.selectionDialogs.SingleSelectionDialogFragment
import io.github.pelmenstar1.digiDict.common.ui.settings.SettingsInflater
import io.github.pelmenstar1.digiDict.common.ui.settings.settingsDescriptor
import io.github.pelmenstar1.digiDict.common.ui.showAlertDialog
import io.github.pelmenstar1.digiDict.databinding.FragmentSettingsBinding
import io.github.pelmenstar1.digiDict.prefs.DigiDictAppPreferences
import io.github.pelmenstar1.digiDict.widgets.ListAppWidget
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.transform
import kotlinx.coroutines.launch

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

            bindActionHandler(ACTION_DELETE_ALL_RECORDS) {
                requestDeleteAllRecords()
            }

            // On lower API levels, there are no break strategy and hyphenation items
            if (Build.VERSION.SDK_INT >= 23) {
                bindTextFormatter(ITEM_BREAK_STRATEGY, ResourcesBreakStrategyStringFormatter(context))
                bindTextFormatter(ITEM_HYPHENATION_FREQUENCY, ResourcesHyphenationStringFormatter(context))

                bindContentItemClickListener(ITEM_BREAK_STRATEGY) { showBreakStrategyDialog() }
                bindContentItemClickListener(ITEM_HYPHENATION_FREQUENCY) { showHyphenationDialog() }
            }
        }

        // On lower API levels these dialog can't be shown.
        if (Build.VERSION.SDK_INT >= 23) {
            initBreakStrategyDialogIfShown()
            initHyphenationDialogIfShown()
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
                settingsInflater.applySnapshot(settingsController, snapshot, contentContainer)
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

    private fun <TValue : Any> initChangePreferenceValueDialog(
        dialog: SingleSelectionDialogFragment<TValue>,
        entry: AppPreferences.Entry<TValue, DigiDictAppPreferences.Entries>
    ) {
        dialog.onValueSelected = { value ->
            viewModel.changePreferenceValue(entry, value)
        }
    }

    @Suppress("UNCHECKED_CAST")
    private fun <TValue : Any> initChangePreferenceValueDialogIfShown(
        tag: String,
        entry: AppPreferences.Entry<TValue, DigiDictAppPreferences.Entries>
    ) {
        childFragmentManager.findFragmentByTag(tag)?.also {
            initChangePreferenceValueDialog(it as SingleSelectionDialogFragment<TValue>, entry)
        }
    }

    private inline fun <TValue : Any> showPreferenceEntryDialogAsync(
        tag: String,
        crossinline getEntry: DigiDictAppPreferences.Entries.() -> AppPreferences.Entry<TValue, DigiDictAppPreferences.Entries>,
        crossinline createDialog: (TValue) -> SingleSelectionDialogFragment<TValue>
    ) {
        lifecycleScope.launch {
            val snapshot = viewModel.dataStateFlow.firstSuccess()
            val entry = DigiDictAppPreferences.Entries.getEntry()
            val value = snapshot[entry]

            createDialog(value).also {
                initChangePreferenceValueDialog(it, entry)

                it.show(childFragmentManager, tag)
            }
        }
    }

    @RequiresApi(23)
    private fun initBreakStrategyDialogIfShown() {
        initChangePreferenceValueDialogIfShown(
            TAG_BREAK_STRATEGY_DIALOG,
            DigiDictAppPreferences.Entries.recordTextBreakStrategy
        )
    }

    @RequiresApi(23)
    private fun initHyphenationDialogIfShown() {
        initChangePreferenceValueDialogIfShown(
            TAG_HYPHENATION_DIALOG,
            DigiDictAppPreferences.Entries.recordTextHyphenationFrequency
        )
    }

    @RequiresApi(23)
    private fun showBreakStrategyDialog() {
        showPreferenceEntryDialogAsync(
            TAG_BREAK_STRATEGY_DIALOG,
            { recordTextBreakStrategy },
            BreakStrategyDialogFragment::create
        )
    }

    @RequiresApi(23)
    private fun showHyphenationDialog() {
        showPreferenceEntryDialogAsync(
            TAG_HYPHENATION_DIALOG,
            { recordTextHyphenationFrequency },
            HyphenationDialogFragment::create
        )
    }

    companion object {
        private const val ACTION_DELETE_ALL_RECORDS = 0
        private const val ITEM_BREAK_STRATEGY = 1
        private const val ITEM_HYPHENATION_FREQUENCY = 2

        private const val TAG_BREAK_STRATEGY_DIALOG = "BreakStrategyDialog"
        private const val TAG_HYPHENATION_DIALOG = "HyphenationDialog"

        private val descriptor = settingsDescriptor {
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

            group(R.string.remindRecords_label) {
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

            group(R.string.settings_widgetTitle) {
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
                        id = ITEM_BREAK_STRATEGY,
                        nameRes = R.string.settings_breakStrategy,
                        preferenceEntry = { recordTextBreakStrategy },
                        clickable = true
                    ) {
                        text()
                    }

                    item(
                        id = ITEM_HYPHENATION_FREQUENCY,
                        nameRes = R.string.settings_hyphenation,
                        preferenceEntry = { recordTextHyphenationFrequency },
                        clickable = true
                    ) {
                        text()
                    }
                }
            }
        }
    }
}