package io.github.pelmenstar1.digiDict.ui.search

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
import dagger.hilt.android.AndroidEntryPoint
import io.github.pelmenstar1.digiDict.databinding.FragmentSearchBinding
import io.github.pelmenstar1.digiDict.utils.launchFlowCollector

@AndroidEntryPoint
class SearchFragment : Fragment() {
    private val viewModel by viewModels<SearchViewModel>()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val context = requireContext()
        val vm = viewModel
        val navController = findNavController()

        val binding = FragmentSearchBinding.inflate(inflater, container, false)

        binding.lifecycleOwner = viewLifecycleOwner
        binding.viewModel = vm
        binding.navController = navController

        val recyclerView = binding.searchRecyclerView
        val adapter = SearchAdapter(onViewRecord = { id ->
            val directions = SearchFragmentDirections.actionSearchToViewRecord(id)

            navController.navigate(directions)
        })

        recyclerView.also {
            it.layoutManager = LinearLayoutManager(context)
            it.addItemDecoration(DividerItemDecoration(context, DividerItemDecoration.VERTICAL))

            it.adapter = adapter
        }

        lifecycleScope.run {
            launchFlowCollector(vm.result) { data ->
                // Scroll to start to show most appropriate results
                recyclerView.scrollToPosition(0)

                adapter.submitData(data)
            }
        }

        return binding.root
    }
}