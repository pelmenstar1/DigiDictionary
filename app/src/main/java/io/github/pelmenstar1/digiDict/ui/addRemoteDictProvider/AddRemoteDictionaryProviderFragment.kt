package io.github.pelmenstar1.digiDict.ui.addRemoteDictProvider

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import io.github.pelmenstar1.digiDict.R
import io.github.pelmenstar1.digiDict.common.StringFormatter
import io.github.pelmenstar1.digiDict.common.android.popBackStackOnSuccess
import io.github.pelmenstar1.digiDict.common.android.showLifecycleAwareSnackbar
import io.github.pelmenstar1.digiDict.common.android.showSnackbarEventHandlerOnError
import io.github.pelmenstar1.digiDict.common.launchFlowCollector
import io.github.pelmenstar1.digiDict.common.toStringOrEmpty
import io.github.pelmenstar1.digiDict.common.ui.launchErrorFlowCollector
import io.github.pelmenstar1.digiDict.common.ui.setEnabledWhenValid
import io.github.pelmenstar1.digiDict.common.ui.setTextIfCharsChanged
import io.github.pelmenstar1.digiDict.databinding.FragmentAddRemoteDictProviderBinding
import kotlinx.coroutines.flow.filterNotNull
import javax.inject.Inject

@AndroidEntryPoint
class AddRemoteDictionaryProviderFragment : Fragment() {
    private val viewModel by viewModels<AddRemoteDictionaryProviderViewModel>()

    @Inject
    lateinit var messageStringFormatter: StringFormatter<AddRemoteDictionaryProviderMessage>

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val navController = findNavController()
        val vm = viewModel
        val context = requireContext()

        val res = context.resources

        val spaceReplacementVariants = res.getString(R.string.addRemoteDictProvider_spaceReplacementInlined)
        val spaceReplacementItems = res.getStringArray(R.array.addRemoteDictProvider_spaceReplacementVariantsUser)

        val binding = FragmentAddRemoteDictProviderBinding.inflate(inflater, container, false)

        val nameInputLayout = binding.addRemoteDictProviderNameInputLayout
        val schemaInputLayout = binding.addRemoteDictProviderSchemaInputLayout

        val nameEditText = binding.addRemoteDictProviderNameInput
        val schemaEditText = binding.addRemoteDictProviderSchemaInput

        val spaceReplacementSpinner = binding.addRemoteDictProviderSpaceReplacementSpinner
        val addButton = binding.addRemoteDictProviderAdd

        popBackStackOnSuccess(vm.addAction, navController)
        showSnackbarEventHandlerOnError(
            vm.addAction,
            container,
            msgId = R.string.dbError,
            anchorView = addButton
        )

        addButton.setOnClickListener { vm.add() }

        nameEditText.addTextChangedListener { vm.name = it.toStringOrEmpty() }
        schemaEditText.addTextChangedListener { vm.schema = it.toStringOrEmpty() }

        spaceReplacementSpinner.apply {
            adapter = ArrayAdapter(
                context,
                android.R.layout.simple_spinner_dropdown_item,
                spaceReplacementItems
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

        lifecycleScope.run {
            addButton.setEnabledWhenValid(vm.validityFlow, scope = this)

            launchErrorFlowCollector(nameInputLayout, vm.nameErrorFlow, messageStringFormatter)
            launchErrorFlowCollector(schemaInputLayout, vm.schemaErrorFlow, messageStringFormatter)

            launchFlowCollector(vm.nameFlow) {
                nameEditText.setTextIfCharsChanged(it)
            }

            launchFlowCollector(vm.schemaFlow) {
                schemaEditText.setTextIfCharsChanged(it)
            }

            launchFlowCollector(vm.spaceReplacementFlow) {
                val index = spaceReplacementVariants.indexOf(it)

                if (index >= 0) {
                    spaceReplacementSpinner.setSelection(index)
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

        return binding.root
    }
}