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
import io.github.pelmenstar1.digiDict.MessageMapper
import io.github.pelmenstar1.digiDict.R
import io.github.pelmenstar1.digiDict.databinding.FragmentAddRemoteDictProviderBinding
import io.github.pelmenstar1.digiDict.utils.*
import javax.inject.Inject

@AndroidEntryPoint
class AddRemoteDictionaryProviderFragment : Fragment() {
    private val viewModel by viewModels<AddRemoteDictionaryProviderViewModel>()

    @Inject
    lateinit var messageMapper: MessageMapper<AddRemoteDictionaryProviderMessage>

    private lateinit var binding: FragmentAddRemoteDictProviderBinding

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val navController = findNavController()
        val vm = viewModel
        val context = requireContext()

        val binding = FragmentAddRemoteDictProviderBinding.inflate(inflater, container, false)

        vm.apply {
            onSuccessfulAddition.setPopBackStackHandler(navController)

            onValidityCheckError.handler = {
                if (container != null) {
                    val errorMsg = messageMapper.map(AddRemoteDictionaryProviderMessage.DB_ERROR)

                    Snackbar
                        .make(container, errorMsg, Snackbar.LENGTH_INDEFINITE)
                        .setAction(R.string.retry) {
                            restartValidityCheck()
                        }
                        .showLifecycleAwareSnackbar(lifecycle)
                }
            }

            onAdditionError.handler = {
                if (container != null) {
                    val errorMsg = messageMapper.map(AddRemoteDictionaryProviderMessage.DB_ERROR)

                    Snackbar
                        .make(container, errorMsg, Snackbar.LENGTH_SHORT)
                        .setAnchorView(binding.addRemoteDictProviderAdd)
                        .showLifecycleAwareSnackbar(lifecycle)
                }
            }
        }

        val nameInputLayout = binding.addRemoteDictProviderNameInputLayout
        val schemaInputLayout = binding.addRemoteDictProviderSchemaInputLayout

        lifecycleScope.run {
            launchErrorFlowCollector(nameInputLayout, vm.nameErrorFlow, messageMapper)
            launchErrorFlowCollector(schemaInputLayout, vm.schemaErrorFlow, messageMapper)

            launchFlowCollector(vm.validityErrorFlow) {
                val mask = AddRemoteDictionaryProviderViewModel.ALL_VALID_MASK

                // Check if all validity bits are set.
                binding.addRemoteDictProviderAdd.isEnabled = (it and mask) == mask
            }

            launchFlowCollector(vm.isNameEnabledFlow) { nameInputLayout.isEnabled = it }
            launchFlowCollector(vm.isSchemaEnabledFlow) { schemaInputLayout.isEnabled = it }
        }

        binding.run {
            addRemoteDictProviderAdd.setOnClickListener { vm.add() }

            addRemoteDictProviderNameInputLayout.addTextChangedListenerToString { vm.name = it }
            addRemoteDictProviderSchemaInputLayout.addTextChangedListenerToString { vm.schema = it }

            addRemoteDictProviderSpaceReplacementSpinner.apply {
                val res = context.resources

                val spaceReplacementVariantsInlined =
                    res.getString(R.string.addRemoteDictProvider_spaceReplacementInlined)
                val items = context.resources.getStringArray(R.array.addRemoteDictProvider_spaceReplacementVariantsUser)

                adapter = ArrayAdapter(
                    context,
                    android.R.layout.simple_spinner_dropdown_item,
                    items
                )

                onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                    override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                        if (position < spaceReplacementVariantsInlined.length) {
                            vm.spaceReplacement = spaceReplacementVariantsInlined[position]
                        }
                    }

                    override fun onNothingSelected(parent: AdapterView<*>?) {
                    }
                }
            }
        }

        this.binding = binding

        return binding.root
    }
}