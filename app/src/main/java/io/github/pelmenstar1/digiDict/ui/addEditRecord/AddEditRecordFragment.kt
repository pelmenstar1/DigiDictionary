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
import io.github.pelmenstar1.digiDict.MessageMapper
import io.github.pelmenstar1.digiDict.R
import io.github.pelmenstar1.digiDict.data.ComplexMeaning
import io.github.pelmenstar1.digiDict.data.Record
import io.github.pelmenstar1.digiDict.databinding.FragmentAddEditRecordBinding
import io.github.pelmenstar1.digiDict.utils.*
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

        val binding = FragmentAddEditRecordBinding.inflate(inflater, container, false)
        this.binding = binding

        val vm = viewModel
        val recordId = args.recordId

        if (recordId >= 0) {
            // If there's a record to load, inputs should be temporarily disabled and then when the record
            // is successfully loaded, they should be re-enabled.
            setInputsEnabled(false)
        }

        vm.currentRecordId = recordId

        // Init errors only after currentRecordId is set.
        vm.initErrors()
        vm.onRecordSuccessfullyAdded.setPopBackStackHandler(navController)

        registerCollectors(container)
        initMeaning(container)
        initViews()

        return binding.root
    }

    private fun setInputsEnabled(value: Boolean) {
        binding.run {
            addExpressionExpressionInputLayout.isEnabled = value
            addExpressionMeaningListInteraction.isEnabled = value
            addExpressionAdditionalNotesInputLayout.isEnabled = value
        }
    }

    private fun setRecord(value: Record) {
        setRecord(
            value.expression,
            ComplexMeaning.parse(value.rawMeaning),
            value.additionalNotes
        )
    }

    private fun setRecord(
        expression: String,
        meaning: ComplexMeaning,
        notes: String
    ) {
        binding.run {
            addExpressionExpressionInputLayout.setText(expression)
            addExpressionAdditionalNotesInputLayout.setText(notes)
        }

        binding.addExpressionMeaningListInteraction.meaning = meaning
    }

    private fun initViews() {
        binding.run {
            val vm = viewModel

            addExpressionExpressionInputLayout.addTextChangedListener {
                vm.expression = it
            }

            addExpressionAdditionalNotesInputLayout.addTextChangedListener {
                vm.additionalNotes = it
            }

            addExpressionAddButton.run {
                val textId = if (args.recordId >= 0) R.string.edit else R.string.addRecord
                text = resources.getString(textId)

                setOnClickListener {
                    vm.addOrEditExpression()
                }
            }

            addExpressionSearchExpression.setOnClickListener {
                val directions = AddEditRecordFragmentDirections.actionAddEditRecordToChooseRemoteDictionaryProvider(
                    vm.expression.toString()
                )

                findNavController().navigate(directions)
            }

            lifecycleScope.run {
                launchFlowCollector(vm.expressionErrorFlow) {
                    addExpressionExpressionInputLayout.error = it?.let(messageMapper::map)
                    addExpressionSearchExpression.isEnabled = it == null
                }

                launchErrorFlowCollector(addExpressionExpressionInputLayout, vm.expressionErrorFlow, messageMapper)

                launchFlowCollector(vm.invalidity) {
                    addExpressionAddButton.isEnabled = it == 0
                }
            }
        }
    }

    private fun initMeaning(container: ViewGroup?) {
        val vm = viewModel

        val listInteractionView = binding.addExpressionMeaningListInteraction.also {
            it.onErrorStateChanged = { isError ->
                vm.invalidity.withBit(
                    AddEditRecordViewModel.MEANING_INVALIDITY_BIT,
                    isError
                )
            }

            if (container != null) {
                it.onTooManyItems = {
                    Snackbar.make(
                        container,
                        messageMapper.map(AddEditRecordMessage.TOO_MANY_MEANING_LIST_ITEMS),
                        Snackbar.LENGTH_LONG
                    ).showLifecycleAwareSnackbar(lifecycle)
                }
            }
        }

        // Only if there's no 'current record', specify error state for meaning list.
        // Otherwise, wait until 'current record' is set.
        // This will work, because meaning of 'current record' can't be empty.
        if (vm.currentRecordId < 0) {
            listInteractionView.refreshErrorState()
        }

        vm.getMeaning = { listInteractionView.meaning }
    }

    private fun registerCollectors(container: ViewGroup?) {
        val vm = viewModel

        launchMessageFlowCollector(vm.dbErrorFlow, messageMapper, container)

        lifecycleScope.run {
            launchFlowCollector(vm.currentRecordFlow) {
                it?.fold(
                    onSuccess = { record ->
                        if (record != null) {
                            setRecord(record)

                            // Enable inputs after we know that current record has been successfully loaded
                            setInputsEnabled(true)
                        }
                    },
                    onFailure = {
                        if (container != null) {
                            val msg = messageMapper.map(AddEditRecordMessage.LOADING_ERROR)

                            Snackbar
                                .make(container, msg, Snackbar.LENGTH_INDEFINITE)
                                .setAction(R.string.retry) { vm.loadCurrentRecord() }
                                .showLifecycleAwareSnackbar(lifecycle)
                        }
                    }
                )
            }
        }
    }
}