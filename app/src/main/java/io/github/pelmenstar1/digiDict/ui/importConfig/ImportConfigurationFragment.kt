package io.github.pelmenstar1.digiDict.ui.importConfig

import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import io.github.pelmenstar1.digiDict.R
import io.github.pelmenstar1.digiDict.backup.BackupFormat
import io.github.pelmenstar1.digiDict.backup.importing.ImportException
import io.github.pelmenstar1.digiDict.common.fileExtensionOrNull
import io.github.pelmenstar1.digiDict.common.launchFlowCollector
import io.github.pelmenstar1.digiDict.common.ui.NO_OP_DIALOG_ON_CLICK_LISTENER
import io.github.pelmenstar1.digiDict.common.ui.ProgressIndicatorDialogManager
import io.github.pelmenstar1.digiDict.databinding.FragmentImportConfigurationBinding

@AndroidEntryPoint
class ImportConfigurationFragment : Fragment() {
    private val viewModel by viewModels<ImportConfigurationViewModel>()
    private val openDocumentContract = ActivityResultContracts.OpenDocument()
    private val progressIndicatorDialogManager = ProgressIndicatorDialogManager()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val vm = viewModel

        val binding = FragmentImportConfigurationBinding.inflate(inflater, container, false)

        val openDocumentLauncher = registerForActivityResult(openDocumentContract) { uri ->
            if (uri != null) {
                val extension = uri.fileExtensionOrNull()
                val format = extension?.let(BackupFormat::fromExtensionOrNull)

                if (format != null) {
                    startImport(uri, format)
                } else {
                    ImportFormatSelectorDialog().also {
                        it.onFormatSelected = { format ->
                            startImport(uri, format)
                        }

                        it.show(childFragmentManager, null)
                    }
                }
            }
        }

        binding.importConfigImportBadges.setOnCheckedChangeListener { _, isChecked ->
            vm.importBadges = isChecked
        }

        binding.importConfigReplaceBadges.setOnCheckedChangeListener { _, isChecked ->
            vm.replaceBadges = isChecked
        }

        binding.importConfigSelectFile.setOnClickListener {
            openDocumentLauncher.launch(ANY_MIME_TYPE)
        }

        lifecycleScope.run {
            launchFlowCollector(vm.isReplaceBadgesEnabled) { isEnabled ->
                binding.importConfigReplaceBadges.isEnabled = isEnabled
                binding.importConfigReplaceBadgesHelpText.isVisible = isEnabled
            }

            launchFlowCollector(vm.importAction.successFlow) {
                if (container != null) {
                    Snackbar
                        .make(container, R.string.importConfig_successMessage, Snackbar.LENGTH_LONG)
                        .show()
                }

                findNavController().popBackStack()
            }

            launchFlowCollector(vm.importAction.errorFlow) { cause ->
                val messageId = if (cause is ImportException) {
                    when (cause.reason) {
                        ImportException.REASON_DATA_VALIDATION -> R.string.importConfig_errorMessage_validation
                        ImportException.REASON_UNKNOWN_VERSION -> R.string.importConfig_errorMessage_unknownVersion
                        else -> R.string.importConfig_errorMessage_internal
                    }
                } else {
                    R.string.importConfig_errorMessage_internal
                }

                MaterialAlertDialogBuilder(requireContext())
                    .setMessage(messageId)
                    .setPositiveButton(android.R.string.ok, NO_OP_DIALOG_ON_CLICK_LISTENER)
                    .show()

                progressIndicatorDialogManager.cancel()
            }
        }

        progressIndicatorDialogManager.init(this, vm.operationProgressFlow)

        return binding.root
    }

    private fun startImport(uri: Uri, format: BackupFormat) {
        viewModel.importAction.run(requireContext(), uri, format)
        progressIndicatorDialogManager.showDialog()
    }

    companion object {
        private val ANY_MIME_TYPE = arrayOf("*/*")
    }
}