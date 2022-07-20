package io.github.pelmenstar1.digiDict.ui.viewRecord

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import io.github.pelmenstar1.digiDict.MessageMapper
import io.github.pelmenstar1.digiDict.R
import io.github.pelmenstar1.digiDict.RecordDateTimeFormatter
import io.github.pelmenstar1.digiDict.databinding.FragmentViewRecordBinding
import io.github.pelmenstar1.digiDict.utils.launchMessageFlowCollector
import io.github.pelmenstar1.digiDict.utils.showLifecycleAwareSnackbar
import io.github.pelmenstar1.digiDict.utils.waitUntilDialogAction
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class ViewRecordFragment : Fragment() {
    private val args by navArgs<ViewRecordFragmentArgs>()
    private val viewModel by viewModels<ViewRecordViewModel>()

    @Inject
    lateinit var messageMapper: MessageMapper<ViewRecordMessage>

    private lateinit var dateTimeFormatter: RecordDateTimeFormatter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val vm = viewModel
        val context = requireContext()

        val binding = FragmentViewRecordBinding.inflate(inflater, container, false)
        binding.viewModel = vm
        binding.navController = findNavController()

        dateTimeFormatter = RecordDateTimeFormatter(context)

        vm.id = args.id
        vm.onDeleteConfirmation = {
            waitUntilDialogAction { confirm ->
                MaterialAlertDialogBuilder(context)
                    .setMessage(R.string.deleteRecordMessage)
                    .setPositiveButton(android.R.string.ok) { _, _ -> confirm(true) }
                    .setNegativeButton(android.R.string.cancel) { _, _ -> confirm(false) }
                    .setOnDismissListener { confirm(false) }
                    .create()
            }
        }

        lifecycleScope.run {
            launchMessageFlowCollector(vm.messageFlow, messageMapper, container)

            launch {
                vm.recordFlow.collect {
                    it?.fold(
                        onSuccess = { record ->
                            if (record != null) {
                                binding.record = record

                                binding.viewRecordDateTime.text = dateTimeFormatter.format(record.epochSeconds)
                            }
                        },
                        onFailure = {
                            if (container != null) {
                                val msg = messageMapper.map(ViewRecordMessage.DB_ERROR)

                                Snackbar.make(container, msg, Snackbar.LENGTH_INDEFINITE)
                                    .setAction(R.string.retry) { vm.refreshRecord() }
                                    .showLifecycleAwareSnackbar(lifecycle)
                            }
                        }
                    )
                }
            }
        }

        return binding.root
    }
}