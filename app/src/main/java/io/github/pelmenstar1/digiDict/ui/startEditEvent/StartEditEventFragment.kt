package io.github.pelmenstar1.digiDict.ui.startEditEvent

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
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
import io.github.pelmenstar1.digiDict.common.ui.addTextChangedListener
import io.github.pelmenstar1.digiDict.common.ui.launchErrorFlowCollector
import io.github.pelmenstar1.digiDict.common.ui.setEnabledWhenValid
import io.github.pelmenstar1.digiDict.common.ui.setText
import io.github.pelmenstar1.digiDict.databinding.FragmentStartEditEventBinding
import javax.inject.Inject

@AndroidEntryPoint
class StartEditEventFragment : Fragment() {
    private val viewModel by viewModels<StartEditEventViewModel>()
    private val args by navArgs<StartEditEventFragmentArgs>()

    private var currentEventErrorSnackbar: Snackbar? = null

    @Inject
    lateinit var errorStringFormatter: StringFormatter<StartEditEventError>

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val vm = viewModel
        val ls = lifecycleScope
        val navController = findNavController()
        val currentEventId = args.currentEventId

        val binding = FragmentStartEditEventBinding.inflate(inflater, container, false)

        val nameInputLayout = binding.startEditEventNameInputLayout
        val actionButton = binding.startEditEventActionButton

        popBackStackOnSuccess(vm.startOrEditAction, navController)
        showSnackbarEventHandlerOnError(
            vm.startOrEditAction,
            container,
            msgId = if (currentEventId >= 0) R.string.startEditEvent_editError else R.string.startEditEvent_startError,
            anchorView = actionButton
        )

        vm.currentEventId = currentEventId
        setAppropriateTitle(currentEventId)

        actionButton.also {
            it.setActionButtonText(currentEventId)
            it.setEnabledWhenValid(vm.validity, ls)

            it.setOnClickListener {
                vm.startOrEdit()
            }
        }

        nameInputLayout.also {
            it.addTextChangedListener { text ->
                vm.name = text
            }
        }

        ls.launchErrorFlowCollector(nameInputLayout, vm.nameErrorFlow, errorStringFormatter)

        if (currentEventId >= 0) {
            ls.launchFlowCollector(vm.currentEventStateFlow) {
                when (it) {
                    is DataLoadState.Loading -> {
                        nameInputLayout.isEnabled = false
                    }
                    is DataLoadState.Error -> {
                        nameInputLayout.isEnabled = false

                        if (container != null) {
                            currentEventErrorSnackbar = Snackbar
                                .make(container, R.string.eventLoadError, Snackbar.LENGTH_INDEFINITE)
                                .setAnchorView(actionButton)
                                .setAction(io.github.pelmenstar1.digiDict.common.ui.R.string.retry) {
                                    vm.retryLoadCurrentEvent()
                                }.also { snackbar ->
                                    snackbar.showLifecycleAwareSnackbar(lifecycle)
                                }
                        }
                    }
                    is DataLoadState.Success -> {
                        val (event) = it

                        currentEventErrorSnackbar?.dismiss()
                        currentEventErrorSnackbar = null

                        nameInputLayout.isEnabled = true
                        nameInputLayout.setText(event.name)
                    }
                }
            }
        }

        return binding.root
    }

    private fun setAppropriateTitle(currentEventId: Int) {
        val resId = if (currentEventId >= 0) {
            R.string.startEditEvent_editModeTitle
        } else {
            R.string.startEditEvent_startModeTitle
        }

        (requireActivity() as AppCompatActivity).supportActionBar?.setTitle(resId)
    }

    private fun Button.setActionButtonText(currentEventId: Int) {
        val resId = if (currentEventId >= 0) {
            R.string.applyChanges
        } else {
            R.string.start
        }

        setText(resId)
    }
}