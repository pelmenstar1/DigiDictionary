package io.github.pelmenstar1.digiDict.ui.chooseRemoteDictProvider

import android.content.ComponentName
import android.content.Intent
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
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import io.github.pelmenstar1.digiDict.R
import io.github.pelmenstar1.digiDict.data.RemoteDictionaryProviderInfo
import io.github.pelmenstar1.digiDict.databinding.FragmentChooseRemoteDictProviderBinding
import io.github.pelmenstar1.digiDict.utils.launchFlowCollector
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@AndroidEntryPoint
class ChooseRemoteDictionaryProviderFragment : Fragment() {
    private val args by navArgs<ChooseRemoteDictionaryProviderFragmentArgs>()
    private val viewModel by viewModels<ChooseRemoteDictionaryProviderViewModel>()

    private lateinit var navController: NavController

    // Before the actual value is loaded, suppose the feature is disabled.
    @Volatile
    private var useCustomTabs = false

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

        binding.chooseRemoteDictProviderRecyclerView.also {
            it.adapter = adapter
            it.layoutManager = LinearLayoutManager(context)
            it.addItemDecoration(DividerItemDecoration(context, DividerItemDecoration.VERTICAL))
        }

        lifecycleScope.run {
            launchFlowCollector(viewModel.providers.catch {
                if (container != null) {
                    Snackbar
                        .make(
                            container,
                            R.string.chooseRemoteDictProvider_failedToLoadProvidersError,
                            Snackbar.LENGTH_LONG
                        )
                        .show()
                }

                emit(RemoteDictionaryProviderInfo.PREDEFINED_PROVIDERS)
            }) { providers ->
                binding.chooseRemoteDictProviderLoadingIndicator.visibility = View.GONE

                adapter.submitItems(providers)
            }

            launch(Dispatchers.Default) {
                try {
                    val isCustomTabsEnabled = viewModel.useCustomTabs().also {
                        useCustomTabs = it
                    }

                    if (isCustomTabsEnabled) {
                        withContext(Dispatchers.Main) {
                            bindCustomTabsClient()
                        }
                    }
                } catch (e: Exception) {
                    // If there's an error while loading useCustomTabs() value,
                    // Custom Tabs won't be enabled. The user still will be able to view the page.
                    Log.e(TAG, "during useCustomTabs() loading", e)
                }
            }
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
        val packageName = CustomTabsClient.getPackageName(context, null)

        if (packageName != null) {
            Log.i(TAG, "Binding custom tabs service (package=$packageName)")
            CustomTabsClient.bindCustomTabsService(context, packageName, connection)
        } else {
            Log.i(TAG, "No package found with Custom Tabs support")
        }
    }

    private fun openUrl(provider: RemoteDictionaryProviderInfo, query: String) {
        val context = requireContext()

        val url = Uri.parse(provider.resolvedUrl(query))

        if (useCustomTabs) {
            val intent = CustomTabsIntent.Builder()
                .apply {
                    customTabsSession?.let { setSession(it) }
                }
                .build()

            intent.launchUrl(context, url)
        } else {
            val intent = Intent().apply {
                action = Intent.ACTION_VIEW

                data = url
            }

            context.startActivity(intent)
        }

        viewModel.onRemoteDictionaryProviderUsed(provider)

        isBrowserLaunched = true
    }

    companion object {
        private const val TAG = "ChooseRDPFragment"
    }
}