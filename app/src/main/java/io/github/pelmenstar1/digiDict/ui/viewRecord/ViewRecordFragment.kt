package io.github.pelmenstar1.digiDict.ui.viewRecord

import android.icu.text.DisplayContext
import android.icu.text.SimpleDateFormat
import android.os.Build
import android.os.Bundle
import android.text.format.DateFormat
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
import io.github.pelmenstar1.digiDict.databinding.FragmentViewRecordBinding
import io.github.pelmenstar1.digiDict.utils.getLocaleCompat
import io.github.pelmenstar1.digiDict.utils.launchMessageFlowCollector
import io.github.pelmenstar1.digiDict.utils.showLifecycleAwareSnackbar
import io.github.pelmenstar1.digiDict.utils.waitUntilDialogAction
import kotlinx.coroutines.launch
import java.util.*
import javax.inject.Inject

@AndroidEntryPoint
class ViewRecordFragment : Fragment() {
    private val args by navArgs<ViewRecordFragmentArgs>()
    private val viewModel by viewModels<ViewRecordViewModel>()

    @Inject
    lateinit var messageMapper: MessageMapper<ViewRecordMessage>

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val vm = viewModel

        val binding = FragmentViewRecordBinding.inflate(inflater, container, false)
        binding.viewModel = vm
        binding.navController = findNavController()

        vm.id = args.id
        vm.onDeleteConfirmation = {
            waitUntilDialogAction { confirm ->
                MaterialAlertDialogBuilder(requireContext())
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

                                binding.viewRecordDateTime.text =
                                    formatDateTime(record.epochSeconds)
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

    private fun formatDateTime(epochSeconds: Long): String {
        val date = Date().apply { time = epochSeconds * 1000 }

        val locale = requireContext().getLocaleCompat()

        // Find best format for locale.
        val format = DateFormat.getBestDateTimePattern(locale, DATE_TIME_FORMAT)

        return if (Build.VERSION.SDK_INT >= 24) {
            val formatter = SimpleDateFormat(format, locale).apply {
                setContext(DisplayContext.CAPITALIZATION_FOR_STANDALONE)
            }

            formatter.format(date)
        } else {
            val formatter = java.text.SimpleDateFormat(format, locale)

            formatter.format(date)
        }
    }

    companion object {
        private const val DATE_TIME_FORMAT = "dd MMMM yyyy HH:mm"
    }

}