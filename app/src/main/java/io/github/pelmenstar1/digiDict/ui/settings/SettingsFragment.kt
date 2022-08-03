package io.github.pelmenstar1.digiDict.ui.settings

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.Spinner
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import dagger.hilt.android.AndroidEntryPoint
import io.github.pelmenstar1.digiDict.BuildConfig
import io.github.pelmenstar1.digiDict.MessageMapper
import io.github.pelmenstar1.digiDict.R
import io.github.pelmenstar1.digiDict.backup.RecordImportExportManager
import io.github.pelmenstar1.digiDict.databinding.FragmentSettingsBinding
import io.github.pelmenstar1.digiDict.prefs.AppPreferences
import io.github.pelmenstar1.digiDict.utils.DataLoadState
import io.github.pelmenstar1.digiDict.utils.launchFlowCollector
import io.github.pelmenstar1.digiDict.utils.launchMessageFlowCollector
import javax.inject.Inject

@AndroidEntryPoint
class SettingsFragment : Fragment() {
    internal val viewModel by viewModels<SettingsViewModel>()

    @Inject
    lateinit var messageMapper: MessageMapper<SettingsMessage>

    private val quizSpinnerOnItemSelectedListener = object : AdapterView.OnItemSelectedListener {
        @Suppress("UNCHECKED_CAST")
        override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
            val key = parent.tag as AppPreferences.Entry<Int>

            viewModel.changePreferenceValue(key, position + 1)
        }

        override fun onNothingSelected(parent: AdapterView<*>?) {
        }
    }

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

        binding.run {
            settingsImport.setOnClickListener { vm.importData(context, navController) }
            settingsExport.setOnClickListener { vm.exportData(context) }

            settingsVersion.text = resources.getString(
                R.string.settings_versionFormat,
                BuildConfig.VERSION_NAME, BuildConfig.BUILD_TYPE
            )

            settingsOpenBrowserInAppSwitch.setOnCheckedChangeListener { _, isChecked ->
                viewModel.changePreferenceValue(AppPreferences.Entries.useCustomTabs, isChecked)
            }

            settingsErrorContainer.setOnRetryListener {
                vm.retryLoadPreferences()
            }

            settingsScorePointsPerCorrectAnswerSpinner.initAsQuizScore { scorePointsPerCorrectAnswer }
            settingsScorePointsPerWrongAnswerSpinner.initAsQuizScore { scorePointsPerWrongAnswer }
        }

        lifecycleScope.run {
            launchMessageFlowCollector(viewModel.messageFlow, messageMapper, container)

            launchFlowCollector(viewModel.preferencesSnapshotStateFlow) {
                with(binding) {
                    when (it) {
                        is DataLoadState.Loading -> {
                            settingsLoadingIndicator.visibility = View.VISIBLE
                            settingsErrorContainer.visibility = View.GONE
                            settingsContentContainer.visibility = View.GONE
                        }
                        is DataLoadState.Error -> {
                            settingsErrorContainer.visibility = View.VISIBLE
                            settingsLoadingIndicator.visibility = View.GONE
                            settingsContentContainer.visibility = View.GONE
                        }
                        is DataLoadState.Success -> {
                            settingsContentContainer.visibility = View.VISIBLE
                            settingsLoadingIndicator.visibility = View.GONE
                            settingsErrorContainer.visibility = View.GONE

                            val (snapshot) = it

                            settingsScorePointsPerCorrectAnswerSpinner.setValue(snapshot.scorePointsPerCorrectAnswer)
                            settingsScorePointsPerWrongAnswerSpinner.setValue(snapshot.scorePointsPerWrongAnswer)
                            settingsOpenBrowserInAppSwitch.isChecked = snapshot.useCustomTabs
                        }
                    }
                }
            }
        }

        return binding.root
    }

    private fun Spinner.setValue(value: Int) {
        setSelection(value - 1)
    }

    private inline fun Spinner.initAsQuizScore(getEntry: AppPreferences.Entries.() -> AppPreferences.Entry<Int>) {
        tag = AppPreferences.Entries.getEntry()
        onItemSelectedListener = quizSpinnerOnItemSelectedListener
    }

    override fun onDestroy() {
        super.onDestroy()

        RecordImportExportManager.release()
    }
}