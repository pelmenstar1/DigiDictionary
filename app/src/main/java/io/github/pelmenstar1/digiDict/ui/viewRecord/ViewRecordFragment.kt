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
import dagger.hilt.android.AndroidEntryPoint
import io.github.pelmenstar1.digiDict.R
import io.github.pelmenstar1.digiDict.common.android.popBackStackOnSuccess
import io.github.pelmenstar1.digiDict.common.android.showSnackbarEventHandlerOnError
import io.github.pelmenstar1.digiDict.common.time.CompatDateTimeFormatter
import io.github.pelmenstar1.digiDict.common.ui.showAlertDialog
import io.github.pelmenstar1.digiDict.databinding.FragmentViewRecordBinding
import io.github.pelmenstar1.digiDict.ui.MeaningTextHelper

@AndroidEntryPoint
class ViewRecordFragment : Fragment() {
    private val args by navArgs<ViewRecordFragmentArgs>()
    private val viewModel by viewModels<ViewRecordViewModel>()

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

        with(binding) {
            viewRecordDelete.setOnClickListener {
                showAlertDialog(messageId = R.string.viewRecord_deleteMessage) {
                    viewModel.delete()
                }
            }

            viewRecordEdit.setOnClickListener {
                val directions = ViewRecordFragmentDirections.actionViewRecordToAddEditRecord(args.id)

                navController.navigate(directions)
            }

            viewRecordContainer.setupLoadStateFlow(lifecycleScope, vm) { record ->
                if (record != null) {
                    viewRecordExpressionView.setValue(record.expression)
                    viewRecordMeaningView.text = MeaningTextHelper.parseToFormattedAndHandleErrors(
                        context,
                        record.meaning
                    )

                    viewRecordAdditionalNotesView.setValue(record.additionalNotes)
                    viewRecordScore.setValue(record.score)
                    viewRecordDateTimeView.text = dateTimeFormatter.format(record.epochSeconds)
                    viewRecordBadgeContainer.setBadges(record.badges)
                }
            }
        }

        vm.id = args.id

        popBackStackOnSuccess(vm.deleteAction, navController)
        showSnackbarEventHandlerOnError(vm.deleteAction, container, R.string.dbError)

        return binding.root
    }

    companion object {
        private const val DATE_TIME_FORMAT = "dd MMMM yyyy HH:mm"
    }
}