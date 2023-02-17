package io.github.pelmenstar1.digiDict.ui.addEditBadge

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import io.github.pelmenstar1.digiDict.R
import io.github.pelmenstar1.digiDict.common.DataLoadState
import io.github.pelmenstar1.digiDict.common.StringFormatter
import io.github.pelmenstar1.digiDict.common.android.popBackStackOnSuccess
import io.github.pelmenstar1.digiDict.common.android.showLifecycleAwareSnackbar
import io.github.pelmenstar1.digiDict.common.android.showSnackbarEventHandlerOnError
import io.github.pelmenstar1.digiDict.common.launchFlowCollector
import io.github.pelmenstar1.digiDict.common.trimToString
import io.github.pelmenstar1.digiDict.common.ui.ColorPaletteView
import io.github.pelmenstar1.digiDict.common.ui.launchErrorFlowCollector
import io.github.pelmenstar1.digiDict.common.ui.setEnabledWhenValid
import io.github.pelmenstar1.digiDict.databinding.FragmentAddEditBadgeBinding
import javax.inject.Inject

@AndroidEntryPoint
class AddEditBadgeFragment : Fragment() {
    private val viewModel by viewModels<AddEditBadgeViewModel>()
    private val args by navArgs<AddEditBadgeFragmentArgs>()

    @Inject
    lateinit var messageStringFormatter: StringFormatter<AddEditBadgeMessage>

    private lateinit var binding: FragmentAddEditBadgeBinding

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val vm = viewModel

        popBackStackOnSuccess(vm.addOrEditAction, findNavController())
        showSnackbarEventHandlerOnError(vm.addOrEditAction, container, R.string.dbError)

        val currentBadgeId = args.currentBadgeId
        vm.currentBadgeId = currentBadgeId

        val binding = FragmentAddEditBadgeBinding.inflate(layoutInflater, container, false)
        this.binding = binding

        initViews(currentBadgeId)
        launchJobs(container, currentBadgeId)

        return binding.root
    }

    private fun initViews(currentBadgeId: Int) {
        val vm = viewModel

        binding.addBadgeDialogColorPalette.run {
            onColorSelectedListener = ColorPaletteView.OnColorSelectedListener { color ->
                vm.outlineColor = color
            }

            if (currentBadgeId < 0) {
                // The change shouldn't be animated because it's initializing the state, actually.
                selectLastColor(animate = false)
            }
        }

        binding.addBadgeDialogNameInput.run {
            addTextChangedListener { text -> vm.name = text.trimToString() }
        }

        binding.addBadgeDialogAddButton.run {
            setText(if (currentBadgeId >= 0) R.string.applyChanges else R.string.add)

            setOnClickListener {
                vm.addOrEditBadge()
            }
        }
    }

    private fun launchJobs(container: ViewGroup?, currentBadgeId: Int) {
        val vm = viewModel
        val nameInputLayout = binding.addBadgeDialogNameInputLayout
        val nameInput = binding.addBadgeDialogNameInput
        val colorPalette = binding.addBadgeDialogColorPalette
        val addButton = binding.addBadgeDialogAddButton

        lifecycleScope.run {
            if (currentBadgeId >= 0) {
                launchFlowCollector(vm.currentBadgeStateFlow) {
                    when (it) {
                        is DataLoadState.Loading -> {
                            setInputsEnabled(false)
                        }
                        is DataLoadState.Error -> {
                            setInputsEnabled(false)

                            if (container != null) {
                                Snackbar
                                    .make(container, R.string.addEditBadge_nameExistsError, Snackbar.LENGTH_INDEFINITE)
                                    .setAction(io.github.pelmenstar1.digiDict.common.ui.R.string.retry) {
                                        vm.retryLoadCurrentBadge()
                                    }
                                    .showLifecycleAwareSnackbar(lifecycle)
                            }
                        }
                        is DataLoadState.Success -> {
                            val (value) = it

                            setInputsEnabled(true)
                            nameInput.setText(value.name)

                            // The change shouldn't be animated, because logically it isn't even a change, it's initializing the state.
                            colorPalette.selectColorOrLast(value.outlineColor, animate = false)
                        }
                    }
                }
            }

            launchErrorFlowCollector(nameInputLayout, vm.nameErrorFlow, messageStringFormatter)
            addButton.setEnabledWhenValid(vm.validity, lifecycleScope)
        }
    }

    private fun setInputsEnabled(state: Boolean) {
        binding.run {
            addBadgeDialogAddButton.isEnabled = state
            addBadgeDialogColorPalette.isEnabled = state
            addBadgeDialogNameInputLayout.isEnabled = state
        }
    }
}