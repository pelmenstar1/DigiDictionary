package io.github.pelmenstar1.digiDict.ui.chooseRemoteDictProvider

import android.content.ComponentName
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.browser.customtabs.CustomTabsClient
import androidx.browser.customtabs.CustomTabsIntent
import androidx.browser.customtabs.CustomTabsServiceConnection
import androidx.browser.customtabs.CustomTabsSession
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavController
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import dagger.hilt.android.AndroidEntryPoint
import io.github.pelmenstar1.digiDict.data.RemoteDictionaryProviderInfo
import io.github.pelmenstar1.digiDict.databinding.FragmentChooseRemoteDictProviderBinding
import io.github.pelmenstar1.digiDict.utils.launchFlowCollector
import kotlinx.coroutines.launch

@AndroidEntryPoint
class ChooseRemoteDictionaryProviderFragment : Fragment() {
    private val args by navArgs<ChooseRemoteDictionaryProviderFragmentArgs>()
    private val viewModel by viewModels<ChooseRemoteDictionaryProviderViewModel>()

    private lateinit var navController: NavController

    private var isBrowserLaunched = false

    private var isCustomTabsServiceConnected = false
    private var customTabsSession: CustomTabsSession? = null

    private val connection = object : CustomTabsServiceConnection() {
        override fun onCustomTabsServiceConnected(name: ComponentName, client: CustomTabsClient) {
            isCustomTabsServiceConnected = true

            client.warmup(0)

            val session = client.newSession(null).also {
                customTabsSession = it
            }

            if (session != null) {
                lifecycleScope.launch {
                    viewModel.getMostUsedProvider()?.let {
                        Log.i(TAG, "Preloading page with schema '${it.schema}'")

                        val uri = Uri.parse(it.resolvedUrl(args.query))

                        session.mayLaunchUrl(uri, null, null)
                    }
                }
            }
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            isCustomTabsServiceConnected = false
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        navController = findNavController()

        val query = args.query
        val context = requireContext()

        val binding = FragmentChooseRemoteDictProviderBinding.inflate(inflater, container, false)
        val adapter = ChooseRemoteDictionaryProviderAdapter(
            onProviderChosen = { openUrl(it, query) }
        )

        adapter.query = query

        bindCustomTabsClient()

        binding.chooseRemoteDictProviderRecyclerView.also {
            it.adapter = adapter
            it.layoutManager = LinearLayoutManager(context)
            it.addItemDecoration(DividerItemDecoration(context, DividerItemDecoration.VERTICAL))
        }

        lifecycleScope.launchFlowCollector(viewModel.providers) { providers ->
            adapter.submitItems(providers)
        }

        return binding.root
    }

    override fun onResume() {
        super.onResume()

        if (isBrowserLaunched) {
            navController.popBackStack()
        }
    }

    override fun onDestroy() {
        super.onDestroy()

        if (isCustomTabsServiceConnected) {
            isCustomTabsServiceConnected = false

            requireContext().unbindService(connection)
        }
    }

    private fun bindCustomTabsClient() {
        val context = requireContext()

        CustomTabsClient.bindCustomTabsService(context, "com.android.chrome", connection)
    }

    private fun openUrl(provider: RemoteDictionaryProviderInfo, query: String) {
        val context = requireContext()

        val url = Uri.parse(provider.resolvedUrl(query))

        val intent = CustomTabsIntent.Builder()
            .apply {
                customTabsSession?.let { setSession(it) }
            }
            .build()

        intent.launchUrl(context, url)

        viewModel.onRemoteDictionaryProviderUsed(provider)

        isBrowserLaunched = true
    }

    companion object {
        private const val TAG = "ChooseRDPFragment"
    }
}