package io.github.pelmenstar1.digiDict.ui.addEditRecord

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import io.github.pelmenstar1.digiDict.R
import io.github.pelmenstar1.digiDict.common.*
import io.github.pelmenstar1.digiDict.common.android.popBackStackOnSuccess
import io.github.pelmenstar1.digiDict.common.android.showLifecycleAwareSnackbar
import io.github.pelmenstar1.digiDict.common.android.showSnackbarEventHandlerOnError
import io.github.pelmenstar1.digiDict.common.ui.launchErrorFlowCollector
import io.github.pelmenstar1.digiDict.common.ui.setEnabledWhenFieldValid
import io.github.pelmenstar1.digiDict.common.ui.setEnabledWhenValid
import io.github.pelmenstar1.digiDict.common.ui.setTextIfCharsChanged
import io.github.pelmenstar1.digiDict.data.ComplexMeaning
import io.github.pelmenstar1.digiDict.data.RecordBadgeDao
import io.github.pelmenstar1.digiDict.databinding.FragmentAddEditRecordBinding
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class AddEditRecordFragment : Fragment() {
    private val args by navArgs<AddEditRecordFragmentArgs>()
    private val viewModel by viewModels<AddEditRecordViewModel>()

    private lateinit var binding: FragmentAddEditRecordBinding

    @Inject
    lateinit var messageStringFormatter: StringFormatter<AddEditRecordMessage>

    @Inject
    lateinit var recordBadgeDao: RecordBadgeDao

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val vm = viewModel
        val recordId = args.recordId

        val binding = FragmentAddEditRecordBinding.inflate(inflater, container, false)
        this.binding = binding

        vm.currentRecordId = recordId
        args.initialExpression?.also { vm.expression = it }

        popBackStackOnSuccess(vm.addOrEditAction, findNavController())
        showSnackbarEventHandlerOnError(vm.addOrEditAction, container, R.string.dbError)

        // If there's no 'current record', currentRecordStateFlow shouldn't be collect at all
        // because as there's no record to load, state of currentRecordStateFlow will always be Loading
        // and the inputs will be disabled.
        if (recordId >= 0) {
            lifecycleScope.launchFlowCollector(vm.currentRecordStateFlow) {
                // If the fragment is in edit mode, inputs should be temporarily disabled and then when the record
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
                        val (value) = it

                        // Expression and additional notes will be updated through other flows. See initViews
                        binding.addRecordMeaningListInteraction.meaning = ComplexMeaning.parse(value.meaning)
                        binding.addRecordBadgeInteraction.badges = value.badges

                        setInputsEnabled(true)
                    }
                }
            }
        }

        initViews(recordId)

        return binding.root
    }

    override fun onResume() {
        super.onResume()

        // When the fragment is resumed, selected badges can be different, so update them.
        val badgeInteraction = binding.addRecordBadgeInteraction
        val badgeIds = badgeInteraction.badges.mapToIntArray { it.id }

        lifecycleScope.launch {
            val updatedBadges = recordBadgeDao.getByIds(badgeIds)

            badgeInteraction.badges = updatedBadges
        }
    }

    private fun setInputsEnabled(value: Boolean) {
        binding.run {
            addRecordExpressionInputLayout.isEnabled = value
            addRecordMeaningListInteraction.isEnabled = value
            addRecordAdditionalNotesInputLayout.isEnabled = value
            addRecordBadgeInteraction.isEnabled = value
        }
    }

    private fun initViews(currentRecordId: Int) {
        binding.run {
            val vm = viewModel
            val ls = lifecycleScope

            val expressionEditText = addRecordExpressionInput
            val additionalNotesEditText = addRecordAdditionalNotesInput
            val doNotChangeCreationDateBox = addRecordDoNotChangeCreationTimeBox

            expressionEditText.addTextChangedListener { vm.expression = it.toStringOrEmpty() }
            additionalNotesEditText.addTextChangedListener { vm.additionalNotes = it.toStringOrEmpty() }

            addRecordAddButton.run {
                text = resources.getString(
                    if (currentRecordId >= 0) R.string.addEditRecord_editButtonText else R.string.addEditRecord_addButtonText
                )

                setOnClickListener { vm.addOrEditRecord() }
            }

            addRecordSearchExpression.setOnClickListener {
                val directions = AddEditRecordFragmentDirections.actionAddEditRecordToChooseRemoteDictionaryProvider(
                    vm.expression.trim()
                )

                findNavController().navigate(directions)
            }

            addRecordMeaningListInteraction.run {
                onErrorStateChanged = { isError ->
                    vm.validity.mutate {
                        set(AddEditRecordViewModel.meaningValidityField, !isError)
                    }
                }

                // Only if there's no 'current record', specify error state for meaning list.
                // Otherwise, wait until 'current record' is set.
                // This will work, because meaning of 'current record' can't be empty.
                if (currentRecordId < 0) {
                    refreshErrorState()
                }

                vm.getMeaning = { meaning }
            }

            addRecordBadgeInteraction.run {
                onGetFragmentManager = { childFragmentManager }

                vm.getBadges = { badges }
            }

            doNotChangeCreationDateBox.also {
                it.isVisible = currentRecordId >= 0

                it.setOnCheckedChangeListener { _, isChecked ->
                    vm.changeCreationTime = !isChecked
                }
            }

            ls.launchFlowCollector(vm.expressionFlow) {
                expressionEditText.setTextIfCharsChanged(it)
            }

            ls.launchFlowCollector(vm.additionalNotesFlow) {
                additionalNotesEditText.setTextIfCharsChanged(it)
            }

            ls.launchFlowCollector(vm.changeCreationTimeFlow) {
                doNotChangeCreationDateBox.isChecked = !it
            }

            ls.launchErrorFlowCollector(addRecordExpressionInputLayout, vm.expressionErrorFlow, messageStringFormatter)

            addRecordSearchExpression.setEnabledWhenFieldValid(
                vm.validity, AddEditRecordViewModel.expressionValidityField,
                ls
            )

            addRecordAddButton.setEnabledWhenValid(vm.validity, ls)
        }
    }
}