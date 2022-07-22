package io.github.pelmenstar1.digiDict.ui.addRemoteDictProvider

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.google.android.material.textfield.TextInputLayout
import dagger.hilt.android.AndroidEntryPoint
import io.github.pelmenstar1.digiDict.MessageMapper
import io.github.pelmenstar1.digiDict.databinding.FragmentAddRemoteDictProviderBinding
import io.github.pelmenstar1.digiDict.utils.launchFlowCollector
import io.github.pelmenstar1.digiDict.utils.launchMessageFlowCollector
import javax.inject.Inject

@AndroidEntryPoint
class AddRemoteDictionaryProviderFragment : Fragment() {
    private val viewModel by viewModels<AddRemoteDictionaryProviderViewModel>()

    @Inject
    lateinit var errorMapper: MessageMapper<AddRemoteDictionaryProviderMessage>

    private lateinit var binding: FragmentAddRemoteDictProviderBinding

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val navController = findNavController()
        val binding = FragmentAddRemoteDictProviderBinding.inflate(inflater, container, false)

        viewModel.onSuccessfulAddition = {
            navController.popBackStack()
        }

        val nameInputLayout = binding.addRemoteDictProviderNameInputLayout
        val schemaInputLayout = binding.addRemoteDictProviderSchemaInputLayout

        lifecycleScope.run {
            launchFlowCollector(viewModel.nameErrorFlow) {
                nameInputLayout.error = it?.let(errorMapper::map)
            }

            launchFlowCollector(viewModel.schemaErrorFlow) {
                schemaInputLayout.error = it?.let(errorMapper::map)
            }

            launchMessageFlowCollector(viewModel.dbErrorFlow, errorMapper, container)
        }

        binding.run {
            addRemoteDictProviderAdd.setOnClickListener { viewModel.add() }

            addRemoteDictProviderNameInputLayout.addTextChangedListener { viewModel.name = it }
            addRemoteDictProviderSchemaInputLayout.addTextChangedListener { viewModel.schema = it }
        }

        savedInstanceState?.let {
            binding.addRemoteDictProviderNameInputLayout.setTextFromBundle(it, STATE_NAME)
            binding.addRemoteDictProviderSchemaInputLayout.setTextFromBundle(it, STATE_SCHEMA)
        }

        this.binding = binding

        return binding.root
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.run {
            putStringFromTextInputLayout(STATE_NAME, binding.addRemoteDictProviderNameInputLayout)
            putStringFromTextInputLayout(STATE_SCHEMA, binding.addRemoteDictProviderSchemaInputLayout)
        }
    }

    companion object {
        private const val STATE_NAME = "io.github.pelmenstar1.digiDict.AddRemoteDictProviderFragment.state.name"
        private const val STATE_SCHEMA = "io.github.pelmenstar1.digiDict.AddRemoteDictProviderFragment.state.schema"

        private fun TextInputLayout.setTextFromBundle(bundle: Bundle, key: String) {
            editText?.let {
                bundle.getString(key)?.let(it::setText)
            }
        }

        private fun Bundle.putStringFromTextInputLayout(key: String, inputLayout: TextInputLayout) {
            putString(key, inputLayout.editText?.text?.toString())
        }

        private inline fun TextInputLayout.addTextChangedListener(crossinline block: (String) -> Unit) {
            editText?.addTextChangedListener {
                block(it?.toString() ?: "")
            }
        }
    }
}