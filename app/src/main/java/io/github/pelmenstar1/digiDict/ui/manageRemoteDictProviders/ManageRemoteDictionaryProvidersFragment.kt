package io.github.pelmenstar1.digiDict.ui.manageRemoteDictProviders

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import io.github.pelmenstar1.digiDict.R
import io.github.pelmenstar1.digiDict.databinding.FragmentManageRemoteDictProvidersBinding
import io.github.pelmenstar1.digiDict.utils.DataLoadState
import io.github.pelmenstar1.digiDict.utils.NO_OP_DIALOG_ON_CLICK_LISTENER
import io.github.pelmenstar1.digiDict.utils.launchFlowCollector
import io.github.pelmenstar1.digiDict.utils.showLifecycleAwareSnackbar

@AndroidEntryPoint
class ManageRemoteDictionaryProvidersFragment : Fragment() {
    private val viewModel by viewModels<ManageRemoteDictionaryProvidersViewModel>()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val context = requireContext()
        val navController = findNavController()
        val vm = viewModel

        val binding = FragmentManageRemoteDictProvidersBinding.inflate(inflater, container, false)

        val adapter = ManageRemoteDictionaryAdapter(
            onDeleteProvider = {
                MaterialAlertDialogBuilder(context)
                    .setMessage(R.string.manageRemoteDictProviders_deleteMessage)
                    .setPositiveButton(android.R.string.ok) { _, _ -> vm.delete(it) }
                    .setNegativeButton(android.R.string.cancel, NO_OP_DIALOG_ON_CLICK_LISTENER)
                    .show()
            }
        )

        with(binding) {
            manageRemoteDictProvidersRecyclerView.also {
                it.adapter = adapter
                it.layoutManager = LinearLayoutManager(context)
                it.addItemDecoration(DividerItemDecoration(context, DividerItemDecoration.VERTICAL))
            }

            manageRemoteDictProvidersAdd.also {
                it.setOnClickListener {
                    val directions =
                        ManageRemoteDictionaryProvidersFragmentDirections.actionManageRemoteDictionaryProvidersToAddRemoteDictionaryProvider()

                    navController.navigate(directions)
                }
            }

            manageRemoteDictProvidersErrorContainer.setOnRetryListener {
                vm.retryLoadProviders()
            }

            vm.onDeleteError.handler = {
                container?.let {
                    Snackbar
                        .make(it, R.string.manageRemoteDictProviders_deleteError, Snackbar.LENGTH_SHORT)
                        .showLifecycleAwareSnackbar(lifecycle)
                }
            }

            lifecycleScope.launchFlowCollector(vm.providersStateFlow) {
                when (it) {
                    is DataLoadState.Loading -> {
                        manageRemoteDictProvidersLoadingIndicator.visibility = View.VISIBLE
                        manageRemoteDictProvidersErrorContainer.visibility = View.GONE
                        manageRemoteDictProvidersRecyclerView.visibility = View.GONE
                        manageRemoteDictProvidersAdd.visibility = View.GONE
                    }
                    is DataLoadState.Error -> {
                        manageRemoteDictProvidersErrorContainer.visibility = View.VISIBLE
                        manageRemoteDictProvidersLoadingIndicator.visibility = View.GONE
                        manageRemoteDictProvidersRecyclerView.visibility = View.GONE
                        manageRemoteDictProvidersAdd.visibility = View.GONE
                    }
                    is DataLoadState.Success -> {
                        val (providers) = it

                        manageRemoteDictProvidersRecyclerView.visibility = View.VISIBLE
                        manageRemoteDictProvidersAdd.visibility = View.VISIBLE
                        manageRemoteDictProvidersErrorContainer.visibility = View.GONE
                        manageRemoteDictProvidersLoadingIndicator.visibility = View.GONE

                        adapter.submitItems(providers)
                    }
                }
            }
        }

        return binding.root
    }
}