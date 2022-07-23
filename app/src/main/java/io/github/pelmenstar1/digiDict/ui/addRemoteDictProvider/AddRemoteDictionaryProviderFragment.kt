package io.github.pelmenstar1.digiDict.ui.addRemoteDictProvider

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import dagger.hilt.android.AndroidEntryPoint
import io.github.pelmenstar1.digiDict.MessageMapper
import io.github.pelmenstar1.digiDict.databinding.FragmentAddRemoteDictProviderBinding
import io.github.pelmenstar1.digiDict.utils.addTextChangedListenerToString
import io.github.pelmenstar1.digiDict.utils.launchErrorFlowCollector
import io.github.pelmenstar1.digiDict.utils.launchMessageFlowCollector
import io.github.pelmenstar1.digiDict.utils.popBackStackLambda
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

        val binding = FragmentAddRemoteDictProviderBinding.inflate(inflater, container, false)

        vm.onSuccessfulAddition = navController.popBackStackLambda()

        val nameInputLayout = binding.addRemoteDictProviderNameInputLayout
        val schemaInputLayout = binding.addRemoteDictProviderSchemaInputLayout

        lifecycleScope.run {
            launchErrorFlowCollector(nameInputLayout, vm.nameErrorFlow, messageMapper)
            launchErrorFlowCollector(schemaInputLayout, vm.schemaErrorFlow, messageMapper)
            launchMessageFlowCollector(viewModel.dbErrorFlow, messageMapper, container)
        }

        binding.run {
            addRemoteDictProviderAdd.setOnClickListener { vm.add() }

            addRemoteDictProviderNameInputLayout.addTextChangedListenerToString { vm.name = it }
            addRemoteDictProviderSchemaInputLayout.addTextChangedListenerToString { vm.schema = it }
        }

        this.binding = binding

        return binding.root
    }
}