package io.github.pelmenstar1.digiDict.ui.settings

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavController
import androidx.navigation.fragment.findNavController
import dagger.hilt.android.AndroidEntryPoint
import io.github.pelmenstar1.digiDict.R
import io.github.pelmenstar1.digiDict.backup.RecordImportExportManager
import io.github.pelmenstar1.digiDict.common.DataLoadState
import io.github.pelmenstar1.digiDict.common.MessageMapper
import io.github.pelmenstar1.digiDict.common.launchFlowCollector
import io.github.pelmenstar1.digiDict.common.launchMessageFlowCollector
import io.github.pelmenstar1.digiDict.databinding.FragmentSettingsBinding
import io.github.pelmenstar1.digiDict.prefs.AppPreferences
import io.github.pelmenstar1.digiDict.ui.settings.SettingsDescriptor.Companion.get
import io.github.pelmenstar1.digiDict.widgets.ListAppWidget
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.transform
import javax.inject.Inject

@AndroidEntryPoint
class SettingsFragment : Fragment() {
    internal val viewModel by viewModels<SettingsViewModel>()

    @Inject
    lateinit var messageMapper: MessageMapper<SettingsMessage>

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val navController = findNavController()
        val vm = viewModel
        val context = requireContext()

        val binding = FragmentSettingsBinding.inflate(inflater, container, false)

        RecordImportExportManager.init(this)

        val contentContainer = binding.settingsContentContainer

        SettingsInflater(context).inflate(
            descriptor,
            onValueChanged = viewModel::changePreferenceValue,
            actionArgs = SettingsDescriptor.ActionArgs(context, vm, navController),
            container = contentContainer
        )

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

        return binding.root
    }

    override fun onDestroy() {
        super.onDestroy()

        RecordImportExportManager.release()
    }

    companion object {
        private val descriptor = settingsDescriptor {
            itemBlock(R.string.quiz) {
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

            itemBlock(R.string.remoteDictProviderShort) {
                item(
                    nameRes = R.string.settings_openBrowserInApp,
                    iconRes = R.drawable.ic_open_browser_in_app,
                    preferenceEntry = { useCustomTabs }
                ) {
                    switch()
                }
            }

            itemBlock(R.string.remindRecords_label) {
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

            itemBlock(R.string.settings_widgetTitle) {
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

            actionBlock(R.string.settings_backupTitle) {
                action(R.string.settings_export) { args ->
                    val context = args.get<Context>()
                    val vm = args.get<SettingsViewModel>()

                    vm.exportData(context)
                }

                action(R.string.settings_import) { args ->
                    val context = args.get<Context>()
                    val vm = args.get<SettingsViewModel>()
                    val navController = args.get<NavController>()

                    vm.importData(context, navController)
                }
            }
        }
    }
}