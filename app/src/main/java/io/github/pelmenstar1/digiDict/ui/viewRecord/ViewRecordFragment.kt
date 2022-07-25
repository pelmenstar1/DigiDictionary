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
import io.github.pelmenstar1.digiDict.ui.MeaningTextHelper
import io.github.pelmenstar1.digiDict.utils.NO_OP_DIALOG_ON_CLICK_LISTENER
import io.github.pelmenstar1.digiDict.utils.popBackStackLambda
import io.github.pelmenstar1.digiDict.utils.setFormattedText
import io.github.pelmenstar1.digiDict.utils.showLifecycleAwareSnackbar
import kotlinx.coroutines.launch
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
        val context = requireContext()
        val res = context.resources
        val navController = findNavController()

        val binding = FragmentViewRecordBinding.inflate(inflater, container, false)

        with(binding) {
            viewRecordDelete.setOnClickListener {
                MaterialAlertDialogBuilder(context)
                    .setMessage(R.string.deleteRecordMessage)
                    .setPositiveButton(android.R.string.ok) { _, _ ->
                        viewModel.delete()
                    }
                    .setNegativeButton(android.R.string.cancel, NO_OP_DIALOG_ON_CLICK_LISTENER)
                    .show()
            }

            viewRecordEdit.setOnClickListener {
                val directions = ViewRecordFragmentDirections.actionViewRecordToAddEditRecord(args.id)

                navController.navigate(directions)
            }
        }

        vm.onRecordDeleted = navController.popBackStackLambda()
        vm.id = args.id

        val dateTimeFormatter = RecordDateTimeFormatter(context)

        val expressionFormat = res.getString(R.string.expressionAndValueFormat)
        val additionalNotesFormat = res.getString(R.string.additionalNotesAndValueFormat)
        val scoreFormat = res.getString(R.string.scoreAndValueFormat)

        vm.onLoadingError = {
            val msg = messageMapper.map(ViewRecordMessage.DB_ERROR)

            container?.let {
                Snackbar.make(it, msg, Snackbar.LENGTH_INDEFINITE)
                    .setAction(R.string.retry) {
                        vm.refreshRecord()
                    }
                    .showLifecycleAwareSnackbar(lifecycle)
            }
        }

        vm.onDeleteError = {
            val msg = messageMapper.map(ViewRecordMessage.DB_ERROR)

            container?.let {
                Snackbar.make(it, msg, Snackbar.LENGTH_LONG)
                    .showLifecycleAwareSnackbar(lifecycle)
            }
        }

        lifecycleScope.launch {
            vm.recordFlow.collect { record ->
                if (record != null) {
                    with(binding) {
                        viewRecordExpression.setFormattedText(expressionFormat, record.expression)
                        viewRecordMeaning.text =
                            MeaningTextHelper.parseToFormatted(record.rawMeaning)
                        viewRecordAdditionalNotes.setFormattedText(
                            additionalNotesFormat,
                            record.additionalNotes
                        )
                        viewRecordScore.setFormattedText(scoreFormat, record.score)

                        viewRecordDateTime.text = dateTimeFormatter.format(record.epochSeconds)
                    }
                }
            }
        }


        return binding.root
    }
}