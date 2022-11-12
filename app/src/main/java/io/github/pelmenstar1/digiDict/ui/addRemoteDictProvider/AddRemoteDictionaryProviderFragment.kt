package io.github.pelmenstar1.digiDict.ui.addRemoteDictProvider

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import io.github.pelmenstar1.digiDict.R
import io.github.pelmenstar1.digiDict.common.*
import io.github.pelmenstar1.digiDict.common.ui.addTextChangedListener
import io.github.pelmenstar1.digiDict.common.ui.launchErrorFlowCollector
import io.github.pelmenstar1.digiDict.databinding.FragmentAddRemoteDictProviderBinding
import kotlinx.coroutines.flow.filterNotNull
import javax.inject.Inject

@AndroidEntryPoint
class AddRemoteDictionaryProviderFragment : Fragment() {
    private val viewModel by viewModels<AddRemoteDictionaryProviderViewModel>()

    @Inject
    lateinit var messageMapper: MessageMapper<AddRemoteDictionaryProviderMessage>

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val navController = findNavController()
        val vm = viewModel
        val context = requireContext()

        val binding = FragmentAddRemoteDictProviderBinding.inflate(inflater, container, false)
        val addButton = binding.addRemoteDictProviderAdd

        vm.apply {
            popBackStackOnSuccess(addAction, navController)
            showSnackbarEventHandlerOnError(
                addAction,
                container,
                msgId = R.string.dbError,
                anchorView = addButton
            )
        }

        val nameInputLayout = binding.addRemoteDictProviderNameInputLayout
        val schemaInputLayout = binding.addRemoteDictProviderSchemaInputLayout

        lifecycleScope.run {
            launchErrorFlowCollector(nameInputLayout, vm.nameErrorFlow, messageMapper)
            launchErrorFlowCollector(schemaInputLayout, vm.schemaErrorFlow, messageMapper)

            launchFlowCollector(vm.validityFlow) { validity ->
                val computedMask = AddRemoteDictionaryProviderViewModel.COMPUTED_MASK
                addButton.isEnabled = if ((validity and computedMask) == computedMask) {
                    validity == AddRemoteDictionaryProviderViewModel.ALL_VALID_MASK
                } else {
                    // To avoid add button blinking when the checking takes too long
                    // we make the button visually enabled but logically disabled. It's correctly handled in the view-model.
                    true
                }
            }

            launchFlowCollector(vm.isInputEnabledFlow) { isEnabled ->
                nameInputLayout.isEnabled = isEnabled
                schemaInputLayout.isEnabled = isEnabled
            }

            launchFlowCollector(vm.validityCheckErrorFlow.filterNotNull()) {
                container?.let {
                    Snackbar.make(container, R.string.dbError, Snackbar.LENGTH_INDEFINITE)
                        .setAction(io.github.pelmenstar1.digiDict.common.ui.R.string.retry) {
                            vm.restartValidityCheck()
                        }
                        .showLifecycleAwareSnackbar(lifecycle)
                }
            }
        }

        binding.run {
            addRemoteDictProviderAdd.setOnClickListener { vm.add() }

            addRemoteDictProviderNameInputLayout.addTextChangedListener { vm.name = it.trimToString() }
            addRemoteDictProviderSchemaInputLayout.addTextChangedListener { vm.schema = it.trimToString() }

            addRemoteDictProviderSpaceReplacementSpinner.apply {
                val res = context.resources

                val spaceReplacementVariants = res.getString(R.string.addRemoteDictProvider_spaceReplacementInlined)
                val items = res.getStringArray(R.array.addRemoteDictProvider_spaceReplacementVariantsUser)

                adapter = ArrayAdapter(
                    context,
                    android.R.layout.simple_spinner_dropdown_item,
                    items
                )

                onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                    override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                        if (position < spaceReplacementVariants.length) {
                            vm.spaceReplacement = spaceReplacementVariants[position]
                        }
                    }

                    override fun onNothingSelected(parent: AdapterView<*>?) {
                    }
                }
            }
        }

        return binding.root
    }
}