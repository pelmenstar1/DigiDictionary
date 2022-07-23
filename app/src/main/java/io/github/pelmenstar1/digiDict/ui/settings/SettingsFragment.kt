package io.github.pelmenstar1.digiDict.ui.settings

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.Spinner
import androidx.datastore.preferences.core.Preferences
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import dagger.hilt.android.AndroidEntryPoint
import io.github.pelmenstar1.digiDict.*
import io.github.pelmenstar1.digiDict.backup.RecordImportExportManager
import io.github.pelmenstar1.digiDict.databinding.FragmentSettingsBinding
import io.github.pelmenstar1.digiDict.utils.launchFlowCollector
import io.github.pelmenstar1.digiDict.utils.launchMessageFlowCollector
import javax.inject.Inject

// TODO: Optimize flow collectors. Don't create new coroutine, create only one that collects Preferences object and then maps its values.

@AndroidEntryPoint
class SettingsFragment : Fragment() {
    private val viewModel by viewModels<SettingsViewModel>()

    @Inject
    lateinit var messageMapper: MessageMapper<SettingsMessage>

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val navController = findNavController()
        val vm = viewModel

        val binding = FragmentSettingsBinding.inflate(inflater, container, false)

        RecordImportExportManager.init(this)

        binding.run {
            settingsImport.setOnClickListener { vm.importData(navController) }
            settingsExport.setOnClickListener { vm.exportData() }

            settingsVersion.text = resources.getString(
                R.string.versionFormat,
                BuildConfig.VERSION_NAME, BuildConfig.BUILD_TYPE
            )

            settingsOpenBrowserInAppSwitch.also { switch ->
                lifecycleScope.launchFlowCollector(viewModel.getPreferenceValueFlow(USE_CUSTOM_TABS_KEY)) {
                    val state = it ?: DEFAULT_USE_CUSTOM_TABS

                    switch.isChecked = state
                }

                switch.setOnCheckedChangeListener { _, isChecked ->
                    viewModel.changePreferenceValue(USE_CUSTOM_TABS_KEY, isChecked)
                }
            }

            initQuizScoreSpinner(
                settingsScorePointsPerCorrectAnswerSpinner,
                SCORE_POINTS_PER_CORRECT_ANSWER_KEY,
                DEFAULT_SCORE_POINTS_PER_CORRECT_ANSWER
            )
            initQuizScoreSpinner(
                settingsScorePointsPerWrongAnswerSpinner,
                SCORE_POINTS_PER_WRONG_ANSWER_KEY,
                DEFAULT_SCORE_POINTS_PER_WRONG_ANSWER
            )
        }

        lifecycleScope.run {
            launchMessageFlowCollector(viewModel.messageFlow, messageMapper, container)
        }

        return binding.root
    }

    private fun initQuizScoreSpinner(
        spinner: Spinner,
        key: Preferences.Key<Int>,
        defaultValue: Int
    ) {
        lifecycleScope.launchFlowCollector(viewModel.getPreferenceValueFlow(key)) {
            val position = (it ?: defaultValue) - 1

            spinner.setSelection(position)
        }

        spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>?,
                view: View?,
                position: Int,
                id: Long
            ) {
                viewModel.changePreferenceValue(key, position + 1)
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {
            }
        }

    }

    override fun onDestroy() {
        super.onDestroy()

        RecordImportExportManager.release()
    }
}