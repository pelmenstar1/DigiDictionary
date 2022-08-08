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
import io.github.pelmenstar1.digiDict.common.ui.addTextChangedListenerToString
import io.github.pelmenstar1.digiDict.databinding.FragmentAddRemoteDictProviderBinding
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
            onSuccessfulAddition.handler = navController.popBackStackEventHandler()

            onValidityCheckError.handler = showSnackbarEventHandler(
                container,
                msgId = R.string.dbError,
                duration = Snackbar.LENGTH_INDEFINITE,
                actionText = io.github.pelmenstar1.digiDict.common.ui.R.string.retry,
                action = { vm.restartValidityCheck() }
            )

            onAdditionError.handler = showSnackbarEventHandler(
                container,
                msgId = R.string.dbError,
                anchorView = binding.addRemoteDictProviderAdd
            )
        }

        val nameInputLayout = binding.addRemoteDictProviderNameInputLayout
        val schemaInputLayout = binding.addRemoteDictProviderSchemaInputLayout

        lifecycleScope.run {
            launchErrorFlowCollector(nameInputLayout, vm.nameErrorFlow, messageMapper)
            launchErrorFlowCollector(schemaInputLayout, vm.schemaErrorFlow, messageMapper)

            launchSetEnabledIfEquals(
                binding.addRemoteDictProviderAdd,
                AddRemoteDictionaryProviderViewModel.ALL_VALID_MASK,
                vm.validityFlow
            )

            launchSetEnabledFlowCollector(nameInputLayout, vm.isNameEnabledFlow)
            launchSetEnabledFlowCollector(schemaInputLayout, vm.isSchemaEnabledFlow)
        }

        binding.run {
            addRemoteDictProviderAdd.setOnClickListener { vm.add() }

            addRemoteDictProviderNameInputLayout.addTextChangedListenerToString { vm.name = it }
            addRemoteDictProviderSchemaInputLayout.addTextChangedListenerToString { vm.schema = it }

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

        this.binding = binding

        return binding.root
    }
}