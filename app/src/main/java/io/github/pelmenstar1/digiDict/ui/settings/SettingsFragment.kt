package io.github.pelmenstar1.digiDict.ui.settings

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.CompoundButton
import android.widget.Spinner
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.google.android.material.switchmaterial.SwitchMaterial
import dagger.hilt.android.AndroidEntryPoint
import io.github.pelmenstar1.digiDict.BuildConfig
import io.github.pelmenstar1.digiDict.R
import io.github.pelmenstar1.digiDict.backup.RecordImportExportManager
import io.github.pelmenstar1.digiDict.common.MessageMapper
import io.github.pelmenstar1.digiDict.common.createNumberRangeList
import io.github.pelmenstar1.digiDict.common.launchMessageFlowCollector
import io.github.pelmenstar1.digiDict.databinding.FragmentSettingsBinding
import io.github.pelmenstar1.digiDict.prefs.AppPreferences
import io.github.pelmenstar1.digiDict.prefs.AppPreferencesGetEntry
import io.github.pelmenstar1.digiDict.widgets.ListAppWidget
import javax.inject.Inject
import kotlin.math.min

@AndroidEntryPoint
class SettingsFragment : Fragment() {
    internal val viewModel by viewModels<SettingsViewModel>()

    @Inject
    lateinit var messageMapper: MessageMapper<SettingsMessage>

    private val rangeSpinnerOnItemSelectedListener = object : AdapterView.OnItemSelectedListener {
        @Suppress("UNCHECKED_CAST")
        override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
            val info = parent.tag as RangeSpinnerInfo

            viewModel.changePreferenceValue(info.entry, info.getAt(position), info.onPrefChanged)
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

            settingsScorePointsPerCorrectAnswerSpinner.initRangeSpinner(
                start = 1,
                endInclusive = 10,
                step = 1,
                entry = { scorePointsPerCorrectAnswer }
            )

            settingsScorePointsPerWrongAnswerSpinner.initRangeSpinner(
                start = 1,
                endInclusive = 10,
                step = 1,
                entry = { scorePointsPerWrongAnswer }
            )

            settingsRemindMaxItemsSpinner.initRangeSpinner(
                start = 10,
                endInclusive = 30,
                step = 5,
                entry = { remindItemsSize }
            )

            settingsWidgetListMaxSizeSpinner.initRangeSpinner(
                start = 10,
                endInclusive = 40,
                step = 5,
                entry = { widgetListMaxSize },
                onPrefChanged = {
                    val appWidgetManager = AppWidgetManager.getInstance(context)
                    val componentName = ComponentName(context, ListAppWidget::class.java)

                    val ids = appWidgetManager.getAppWidgetIds(componentName)

                    ListAppWidget.update(context, appWidgetManager, ids)
                }
            )
        }

        lifecycleScope.run {
            launchMessageFlowCollector(viewModel.messageFlow, messageMapper, container)

            with(binding) {
                settingsContainer.setupLoadStateFlow(this@run, vm) {
                    settingsScorePointsPerCorrectAnswerSpinner.setValue(it.scorePointsPerCorrectAnswer)
                    settingsScorePointsPerWrongAnswerSpinner.setValue(it.scorePointsPerWrongAnswer)
                    settingsRemindMaxItemsSpinner.setValue(it.remindItemsSize)
                    settingsWidgetListMaxSizeSpinner.setValue(it.widgetListMaxSize)
                    settingsOpenBrowserInAppSwitch.isChecked = it.useCustomTabs
                    settingsRemindShowMeaningSwitch.isChecked = it.remindShowMeaning
                }
            }
        }

        return binding.root
    }

    private fun Spinner.setValue(value: Int) {
        val info = tag as RangeSpinnerInfo
        val index = info.indexOf(value)

        setSelection(index)
    }

    private inline fun SwitchMaterial.initSwitch(getEntry: AppPreferencesGetEntry<Boolean>) {
        tag = AppPreferences.Entries.getEntry()
        setOnCheckedChangeListener(switchOnCheckChangedListener)
    }

    private fun Spinner.initRangeSpinner(info: RangeSpinnerInfo) {
        adapter = createSimpleAdapter(info.elements)
        tag = info
        onItemSelectedListener = rangeSpinnerOnItemSelectedListener
    }

    private inline fun Spinner.initRangeSpinner(
        start: Int,
        endInclusive: Int,
        step: Int,
        entry: AppPreferencesGetEntry<Int>,
        noinline onPrefChanged: (() -> Unit)? = null
    ) {
        initRangeSpinner(RangeSpinnerInfo(start, endInclusive, step, AppPreferences.Entries.entry(), onPrefChanged))
    }

    private fun createSimpleAdapter(elements: List<String>): ArrayAdapter<String> {
        return ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item, elements)
    }

    override fun onDestroy() {
        super.onDestroy()

        RecordImportExportManager.release()
    }

    class RangeSpinnerInfo(
        private val start: Int,
        private val endInclusive: Int,
        private val step: Int,
        val entry: AppPreferences.Entry<Int>,
        val onPrefChanged: (() -> Unit)? = null
    ) {
        val elements = createNumberRangeList(start, endInclusive, step)

        fun getAt(position: Int): Int {
            return min(endInclusive, start + position * step)
        }

        fun indexOf(value: Int): Int {
            val constrainedValue = value.coerceIn(start, endInclusive)

            return (constrainedValue - start) / step
        }
    }
}