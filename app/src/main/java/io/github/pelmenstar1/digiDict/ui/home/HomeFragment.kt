package io.github.pelmenstar1.digiDict.ui.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavController
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import dagger.hilt.android.AndroidEntryPoint
import io.github.pelmenstar1.digiDict.databinding.FragmentHomeBinding
import io.github.pelmenstar1.digiDict.utils.launchFlowCollector

@AndroidEntryPoint
class HomeFragment : Fragment() {
    private val viewModel by viewModels<HomeViewModel>()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val navController = findNavController()

        val binding = FragmentHomeBinding.inflate(inflater, container, false)
        initRecyclerView(binding, navController)

        binding.homeAddExpression.setOnClickListener {
            val directions = HomeFragmentDirections.actionHomeToAddEditRecord()

            navController.navigate(directions)
        }

        return binding.root
    }

    private fun initRecyclerView(binding: FragmentHomeBinding, navController: NavController) {
        val context = requireContext()
        val recyclerView = binding.homeRecyclerView

        val adapter = HomeAdapter(onViewRecord = { id ->
            val directions = HomeFragmentDirections.actionHomeToViewRecord(id)

            navController.navigate(directions)
        })

        recyclerView.also {
            it.adapter = adapter
            it.layoutManager = LinearLayoutManager(context)

            it.addItemDecoration(DividerItemDecoration(context, DividerItemDecoration.VERTICAL))
        }

        lifecycleScope.run {
            launchFlowCollector(viewModel.items, adapter::submitData)
        }
    }
}