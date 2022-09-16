package io.github.pelmenstar1.digiDict.ui.exportConfig

import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import io.github.pelmenstar1.digiDict.R
import io.github.pelmenstar1.digiDict.backup.BackupFormat
import io.github.pelmenstar1.digiDict.common.*
import io.github.pelmenstar1.digiDict.common.ui.ProgressIndicatorDialogManager
import io.github.pelmenstar1.digiDict.common.ui.showAlertDialog
import io.github.pelmenstar1.digiDict.databinding.FragmentExportConfigurationBinding
import java.util.*

// TODO: Fix progress dialog
@AndroidEntryPoint
class ExportConfigurationFragment : Fragment() {
    private val viewModel by viewModels<ExportConfigurationViewModel>()
    private val createDocumentContract = ActivityResultContracts.CreateDocument("*/*")
    private val progressIndicatorDialogManager = ProgressIndicatorDialogManager()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val context = requireContext()
        val vm = viewModel

        val binding = FragmentExportConfigurationBinding.inflate(inflater, container, false)
        val createDocLauncher = registerForActivityResult(createDocumentContract) { uri ->
            if (uri != null) {
                val extension = uri.fileExtensionOrNull()

                if (extension == null) {
                    if (container != null) {
                        Snackbar.make(container, R.string.exportConfig_unexpectedUriMessage, Snackbar.LENGTH_LONG)
                            .setAnchorView(binding.exportConfigSelectFileButton)
                            .show()
                    }
                } else {
                    // selectedFormat can't be null here, see a check in exportConfigSelectFileButton's on click listener.
                    val expectedExtension = vm.selectedFormat!!.extension

                    if (extension == expectedExtension) {
                        startExport(uri)
                    } else {
                        val message = resources.getString(
                            R.string.exportConfig_unexpectedFileExtMessage,
                            expectedExtension,
                            extension
                        )

                        showAlertDialog(
                            message,
                            positionButtonAction = { startExport(uri) }
                        )
                    }
                }
            }
        }

        binding.exportConfigExportFormatPicker.let { picker ->
            picker.onItemSelected = { vm.selectedFormat = it }
            picker.setItems(EXPORT_FORMAT_ENTRIES)
        }

        binding.exportConfigExportBadgesCheckbox.setOnCheckedChangeListener { _, isChecked ->
            vm.exportBadges = isChecked
        }

        binding.exportConfigSelectFileButton.setOnClickListener {
            vm.selectedFormat?.let { format ->
                val fileName = createFileName(context.getLocaleCompat(), format)

                createDocLauncher.launch(fileName)
            }
        }

        progressIndicatorDialogManager.init(this, vm.progressFlow)

        lifecycleScope.run {
            launchFlowCollector(vm.exportAction.successFlow) {
                if (container != null) {
                    Snackbar
                        .make(container, R.string.exportConfig_successMessage, Snackbar.LENGTH_LONG)
                        .show()
                }

                findNavController().popBackStack()
            }

            launchFlowCollector(vm.exportAction.errorFlow) {
                if (container != null) {
                    Snackbar
                        .make(container, R.string.exportConfig_errorMessage, Snackbar.LENGTH_LONG)
                        .setAnchorView(binding.exportConfigSelectFileButton)
                        .show()
                }

                progressIndicatorDialogManager.cancel()
            }
        }

        return binding.root
    }

    private fun startExport(uri: Uri) {
        viewModel.exportAction.run(requireContext(), uri)
        progressIndicatorDialogManager.showDialog()
    }

    companion object {
        private val EXPORT_FORMAT_ENTRIES = arrayOf(
            ExportFormatEntry(BackupFormat.DDDB, R.string.exportConfig_ddbbDescription),
            ExportFormatEntry(BackupFormat.JSON, R.string.exportConfig_jsonDescription)
        )

        internal fun createFileName(locale: Locale, format: BackupFormat): String {
            val calendar = Calendar.getInstance(locale)

            return buildString(32) {
                append("digi_dict_")
                appendPaddedTwoDigit(calendar[Calendar.DAY_OF_MONTH])
                append('_')

                // Month is 0-based in Calendar.
                appendPaddedTwoDigit(calendar[Calendar.MONTH] + 1)
                append('_')
                appendPaddedFourDigit(calendar[Calendar.YEAR])

                append('.')
                append(format.extension)
            }
        }
    }
}