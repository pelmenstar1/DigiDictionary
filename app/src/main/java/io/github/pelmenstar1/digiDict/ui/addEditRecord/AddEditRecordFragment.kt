package io.github.pelmenstar1.digiDict.ui.addEditRecord

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import io.github.pelmenstar1.digiDict.R
import io.github.pelmenstar1.digiDict.common.*
import io.github.pelmenstar1.digiDict.common.ui.addTextChangedListener
import io.github.pelmenstar1.digiDict.common.ui.setText
import io.github.pelmenstar1.digiDict.data.ComplexMeaning
import io.github.pelmenstar1.digiDict.data.RecordWithBadges
import io.github.pelmenstar1.digiDict.databinding.FragmentAddEditRecordBinding
import javax.inject.Inject

@AndroidEntryPoint
class AddEditRecordFragment : Fragment() {
    private val args by navArgs<AddEditRecordFragmentArgs>()
    private val viewModel by viewModels<AddEditRecordViewModel>()

    private lateinit var binding: FragmentAddEditRecordBinding

    @Inject
    lateinit var messageMapper: MessageMapper<AddEditRecordMessage>

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val navController = findNavController()

        val vm = viewModel
        val recordId = args.recordId

        val binding = FragmentAddEditRecordBinding.inflate(inflater, container, false)
        this.binding = binding

        vm.currentRecordId = recordId

        // Init errors only after currentRecordId is set.
        vm.initErrors()
        vm.onRecordSuccessfullyAdded.handler = navController.popBackStackEventHandler()
        vm.onAddError.handler = showSnackbarEventHandler(container, R.string.dbError)

        // If there's no 'current record', currentRecordStateFlow shouldn't be collect at all
        // because as there's no record to load, state of currentRecordStateFlow will always be Loading
        // and the inputs will be disabled.
        if (recordId >= 0) {
            lifecycleScope.launchFlowCollector(vm.currentRecordStateFlow) {
                // If there's a record to load, inputs should be temporarily disabled and then when the record
                // is successfully loaded, they should be re-enabled.

                when (it) {
                    is DataLoadState.Loading -> {
                        setInputsEnabled(false)
                    }
                    is DataLoadState.Error -> {
                        setInputsEnabled(false)

                        if (container != null) {
                            Snackbar
                                .make(container, R.string.recordLoadingError, Snackbar.LENGTH_INDEFINITE)
                                .setAction(io.github.pelmenstar1.digiDict.common.ui.R.string.retry) {
                                    vm.retryLoadCurrentRecord()
                                }
                                .showLifecycleAwareSnackbar(lifecycle)
                        }
                    }
                    is DataLoadState.Success -> {
                        it.value?.let { record ->
                            setRecord(record)

                            setInputsEnabled(true)
                        }
                    }
                }
            }
        }

        initMeaningInteraction()
        initBadgeInteraction()
        initViews()

        return binding.root
    }

    private fun setInputsEnabled(value: Boolean) {
        binding.run {
            addRecordExpressionInputLayout.isEnabled = value
            addRecordMeaningListInteraction.isEnabled = value
            addRecordAdditionalNotesInputLayout.isEnabled = value
            addRecordBadgeInteraction.isEnabled = value
        }
    }

    private fun setRecord(value: RecordWithBadges) {
        binding.run {
            addRecordExpressionInputLayout.setText(value.expression)
            addRecordAdditionalNotesInputLayout.setText(value.additionalNotes)
            addRecordMeaningListInteraction.meaning = ComplexMeaning.parse(value.meaning)
            addRecordBadgeInteraction.badges = value.badges
        }
    }

    private fun initViews() {
        binding.run {
            val vm = viewModel

            addRecordExpressionInputLayout.addTextChangedListener {
                vm.expression = it
            }

            addRecordAdditionalNotesInputLayout.addTextChangedListener {
                vm.additionalNotes = it
            }

            addRecordAddButton.run {
                val textId = if (args.recordId >= 0) R.string.edit else R.string.addRecord
                text = resources.getString(textId)

                setOnClickListener { vm.addOrEditRecord() }
            }

            addRecordSearchExpression.setOnClickListener {
                val directions = AddEditRecordFragmentDirections.actionAddEditRecordToChooseRemoteDictionaryProvider(
                    vm.expression.toString()
                )

                findNavController().navigate(directions)
            }

            lifecycleScope.run {
                launchFlowCollector(vm.expressionErrorFlow) {
                    addRecordExpressionInputLayout.error = it?.let(messageMapper::map)
                    addRecordSearchExpression.isEnabled = it == null
                }

                launchErrorFlowCollector(addRecordExpressionInputLayout, vm.expressionErrorFlow, messageMapper)
                launchSetEnabledIfEquals(addRecordAddButton, AddEditRecordViewModel.ALL_VALID_MASK, vm.validity)
            }
        }
    }

    private fun initMeaningInteraction() {
        val vm = viewModel

        binding.addRecordMeaningListInteraction.also {
            it.onErrorStateChanged = { isError ->
                vm.validity.withBitNullable(
                    AddEditRecordViewModel.MEANING_VALIDITY_BIT,
                    !isError
                )
            }

            // Only if there's no 'current record', specify error state for meaning list.
            // Otherwise, wait until 'current record' is set.
            // This will work, because meaning of 'current record' can't be empty.
            if (vm.currentRecordId < 0) {
                it.refreshErrorState()
            }

            vm.getMeaning = { it.meaning }
        }
    }

    private fun initBadgeInteraction() {
        val vm = viewModel

        binding.addRecordBadgeInteraction.also {
            it.onGetFragmentManager = { childFragmentManager }

            vm.getBadges = { it.badges }
        }
    }
}