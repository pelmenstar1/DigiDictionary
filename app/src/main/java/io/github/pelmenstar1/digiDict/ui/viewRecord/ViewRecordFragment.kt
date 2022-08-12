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
import dagger.hilt.android.AndroidEntryPoint
import io.github.pelmenstar1.digiDict.R
import io.github.pelmenstar1.digiDict.common.CompatDateTimeFormatter
import io.github.pelmenstar1.digiDict.common.NO_OP_DIALOG_ON_CLICK_LISTENER
import io.github.pelmenstar1.digiDict.common.popBackStackEventHandler
import io.github.pelmenstar1.digiDict.common.showSnackbarEventHandler
import io.github.pelmenstar1.digiDict.databinding.FragmentViewRecordBinding
import io.github.pelmenstar1.digiDict.ui.MeaningTextHelper

@AndroidEntryPoint
class ViewRecordFragment : Fragment() {
    private val args by navArgs<ViewRecordFragmentArgs>()
    private val viewModel by viewModels<ViewRecordViewModel>()

    private lateinit var binding: FragmentViewRecordBinding

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val vm = viewModel
        val context = requireContext()
        val navController = findNavController()

        val dateTimeFormatter = CompatDateTimeFormatter(context, DATE_TIME_FORMAT)

        val binding = FragmentViewRecordBinding.inflate(inflater, container, false)
        this.binding = binding

        with(binding) {
            viewRecordDelete.setOnClickListener {
                MaterialAlertDialogBuilder(context)
                    .setMessage(R.string.viewRecord_deleteMessage)
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

            viewRecordContainer.setupLoadStateFlow(lifecycleScope, vm) { record ->
                if (record != null) {
                    viewRecordExpressionView.setValue(record.expression)
                    viewRecordMeaningView.text = MeaningTextHelper.parseToFormatted(
                        record.rawMeaning
                    )

                    viewRecordAdditionalNotesView.setValue(record.additionalNotes)
                    viewRecordScore.setValue(record.score)

                    viewRecordDateTimeView.text = dateTimeFormatter.format(record.epochSeconds)
                }
            }
        }

        vm.onRecordDeleted.handler = navController.popBackStackEventHandler()
        vm.id = args.id

        vm.onDeleteError.handler = showSnackbarEventHandler(container, R.string.dbError)

        return binding.root
    }

    companion object {
        private const val DATE_TIME_FORMAT = "dd MMMM yyyy HH:mm"
    }
}