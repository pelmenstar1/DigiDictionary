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
import io.github.pelmenstar1.digiDict.data.RecordBadgeDao
import io.github.pelmenstar1.digiDict.data.RecordWithBadges
import io.github.pelmenstar1.digiDict.databinding.FragmentAddEditRecordBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@AndroidEntryPoint
class AddEditRecordFragment : Fragment() {
    private val args by navArgs<AddEditRecordFragmentArgs>()
    private val viewModel by viewModels<AddEditRecordViewModel>()

    private lateinit var binding: FragmentAddEditRecordBinding

    @Inject
    lateinit var messageMapper: MessageMapper<AddEditRecordMessage>

    @Inject
    lateinit var recordBadgeDao: RecordBadgeDao

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

        popBackStackOnSuccess(vm.addOrEditAction, navController)
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
                        setRecord(it.value)
                        setInputsEnabled(true)
                    }
                }
            }
        }

        initMeaningInteraction()
        initBadgeInteraction()
        initViews()

        // Set the initial expression (if it exists) only when all the listeners are set on the EditText's
        args.initialExpression?.let {
            binding.addRecordExpressionInputLayout.setText(it)
        }

        return binding.root
    }

    override fun onResume() {
        super.onResume()

        // When the fragment is resumed, selected badges can be different, so update them.
        val badgeInteraction = binding.addRecordBadgeInteraction
        val badgeIds = badgeInteraction.badges.mapToIntArray { it.id }

        lifecycleScope.launch {
            val updatedBadges = recordBadgeDao.getByIds(badgeIds)

            withContext(Dispatchers.Main) {
                badgeInteraction.badges = updatedBadges
            }
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
                text = resources.getString(
                    if (args.recordId >= 0) R.string.addEditRecord_editButtonText else R.string.addEditRecord_addButtonText
                )

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

                launchFlowCollector(vm.validity) { value ->
                    val isComputed = (value and AddEditRecordViewModel.VALIDITY_COMPUTED_BIT) != 0
                    val isEnabled = if (isComputed) {
                        value == AddEditRecordViewModel.ALL_VALID_MASK
                    } else {
                        // To avoid add button blinking when the checking takes too long
                        // we make the button visually enabled but logically disabled. It's handled in the viewmodel.
                        true
                    }

                    addRecordAddButton.isEnabled = isEnabled
                }
            }
        }
    }

    private fun initMeaningInteraction() {
        val vm = viewModel

        binding.addRecordMeaningListInteraction.also {
            it.onErrorStateChanged = { isError ->
                vm.validity.update { prevValue ->
                    prevValue.withBit(
                        AddEditRecordViewModel.MEANING_VALIDITY_BIT,
                        !isError
                    )
                }
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

        binding.addRecordBadgeInteraction.also { badgeInteraction ->
            badgeInteraction.onGetFragmentManager = { childFragmentManager }
            vm.getBadges = { badgeInteraction.badges }
        }
    }
}