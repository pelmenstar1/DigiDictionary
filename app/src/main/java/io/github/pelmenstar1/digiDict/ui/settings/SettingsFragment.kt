package io.github.pelmenstar1.digiDict.ui.settings

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.CompoundButton
import android.widget.Spinner
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.google.android.material.switchmaterial.SwitchMaterial
import dagger.hilt.android.AndroidEntryPoint
import io.github.pelmenstar1.digiDict.BuildConfig
import io.github.pelmenstar1.digiDict.MessageMapper
import io.github.pelmenstar1.digiDict.R
import io.github.pelmenstar1.digiDict.backup.RecordImportExportManager
import io.github.pelmenstar1.digiDict.databinding.FragmentSettingsBinding
import io.github.pelmenstar1.digiDict.prefs.AppPreferences
import io.github.pelmenstar1.digiDict.prefs.AppPreferencesGetEntry
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
            val entry = parent.tag as AppPreferences.Entry<Int>

            viewModel.changePreferenceValue(entry, position + 1)
        }

        override fun onNothingSelected(parent: AdapterView<*>?) {
        }
    }

    @Suppress("UNCHECKED_CAST")
    private val switchOnCheckChangedListener = CompoundButton.OnCheckedChangeListener { view, isChecked ->
        val entry = view.tag as AppPreferences.Entry<Boolean>

        viewModel.changePreferenceValue(entry, isChecked)
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

            settingsOpenBrowserInAppSwitch.initSwitch { useCustomTabs }
            settingsRemindShowMeaningSwitch.initSwitch { remindShowMeaning }

            settingsScorePointsPerCorrectAnswerSpinner.initAsQuizScore { scorePointsPerCorrectAnswer }
            settingsScorePointsPerWrongAnswerSpinner.initAsQuizScore { scorePointsPerWrongAnswer }

            settingsRemindMaxItemsSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                    vm.changePreferenceValue(position + 1) { remindItemsSize }
                }

                override fun onNothingSelected(parent: AdapterView<*>?) {
                }
            }
        }

        lifecycleScope.run {
            launchMessageFlowCollector(viewModel.messageFlow, messageMapper, container)

            with(binding) {
                settingsContainer.setupLoadStateFlow(this@run, vm) {
                    settingsScorePointsPerCorrectAnswerSpinner.setValue(it.scorePointsPerCorrectAnswer)
                    settingsScorePointsPerWrongAnswerSpinner.setValue(it.scorePointsPerWrongAnswer)
                    settingsRemindMaxItemsSpinner.setValue(it.remindItemsSize)
                    settingsOpenBrowserInAppSwitch.isChecked = it.useCustomTabs
                    settingsRemindShowMeaningSwitch.isChecked = it.remindShowMeaning
                }
            }
        }

        return binding.root
    }

    private fun Spinner.setValue(value: Int) {
        setSelection(value - 1)
    }

    private inline fun SwitchMaterial.initSwitch(getEntry: AppPreferencesGetEntry<Boolean>) {
        tag = AppPreferences.Entries.getEntry()
        setOnCheckedChangeListener(switchOnCheckChangedListener)
    }

    private inline fun Spinner.initAsQuizScore(getEntry: AppPreferencesGetEntry<Int>) {
        tag = AppPreferences.Entries.getEntry()
        onItemSelectedListener = quizSpinnerOnItemSelectedListener
    }

    override fun onDestroy() {
        super.onDestroy()

        RecordImportExportManager.release()
    }
}