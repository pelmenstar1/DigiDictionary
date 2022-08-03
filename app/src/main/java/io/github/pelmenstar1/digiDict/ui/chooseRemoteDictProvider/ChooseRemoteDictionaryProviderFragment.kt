package io.github.pelmenstar1.digiDict.ui.chooseRemoteDictProvider

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.browser.customtabs.*
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import io.github.pelmenstar1.digiDict.R
import io.github.pelmenstar1.digiDict.data.RemoteDictionaryProviderInfo
import io.github.pelmenstar1.digiDict.databinding.FragmentChooseRemoteDictProviderBinding
import io.github.pelmenstar1.digiDict.utils.debugLog
import io.github.pelmenstar1.digiDict.utils.launchFlowCollector
import io.github.pelmenstar1.digiDict.utils.mapOffset
import io.github.pelmenstar1.digiDict.utils.showLifecycleAwareSnackbar
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@AndroidEntryPoint
class ChooseRemoteDictionaryProviderFragment : Fragment() {
    internal val args by navArgs<ChooseRemoteDictionaryProviderFragmentArgs>()
    internal val viewModel by viewModels<ChooseRemoteDictionaryProviderViewModel>()

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
                    try {
                        val mostUsedProviders = viewModel.getMostUsedProviders()

                        if (mostUsedProviders.isNotEmpty()) {
                            val mostUsedProvider = mostUsedProviders[0]
                            val query = args.query

                            val uri = mostUsedProvider.resolvedUrlParsed(query)
                            val otherLikelyUris = if (mostUsedProviders.size > 1) {
                                mostUsedProviders.mapOffset(1) { provider ->
                                    Bundle(1).also {
                                        it.putParcelable(CustomTabsService.KEY_URL, provider.resolvedUrlParsed(query))
                                    }
                                }
                            } else {
                                null
                            }

                            debugLog(TAG) {
                                info("Preloading page '$uri'")
                            }

                            session.mayLaunchUrl(uri, null, otherLikelyUris)
                        }
                    } catch (e: Exception) {
                        // Just log the exception, nothing bad happened, mayLaunchUrl is not necessary.
                        Log.e(TAG, "during mayLaunchUrl()", e)
                    }
                }
            }
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            isCustomTabsServiceConnected = false
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
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
                showLoadProvidersError()

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

        if (savedInstanceState != null) {
            isBrowserLaunched = savedInstanceState.getBoolean(SAVED_STATE_IS_BROWSER_LAUNCHED, false)
        }

        return binding.root
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)

        outState.putBoolean(SAVED_STATE_IS_BROWSER_LAUNCHED, isBrowserLaunched)
    }

    override fun onResume() {
        super.onResume()

        if (isBrowserLaunched) {
            findNavController().popBackStack()
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
            debugLog(TAG) {
                info("Binding custom tabs service (package=$packageName)")
            }

            CustomTabsClient.bindCustomTabsService(context, packageName, connection)
        } else {
            debugLog(TAG) {
                info("There are no packages with Custom Tabs support")
            }
        }
    }

    private fun openUrl(provider: RemoteDictionaryProviderInfo, query: String) {
        val context = requireContext()

        val url = provider.resolvedUrlParsed(query)

        val isLaunched = try {
            if (useCustomTabs) {
                val intent = CustomTabsIntent.Builder()
                    .apply {
                        customTabsSession?.let { setSession(it) }
                    }
                    .build()

                intent.launchUrl(context, url)
            } else {
                startInDefaultView(context, url)
            }

            true
        } catch (e: Exception) {
            Log.e(TAG, "During launch. Falling back to default view", e)

            startInDefaultView(context, url)
        }

        isBrowserLaunched = isLaunched

        if (isLaunched) {
            viewModel.onRemoteDictionaryProviderUsed(provider)
        }
    }

    /**
     * Returns whether launch of the activity was successful.
     */
    private fun startInDefaultView(context: Context, url: Uri): Boolean {
        return try {
            val intent = Intent().apply {
                action = Intent.ACTION_VIEW

                data = url
            }

            context.startActivity(intent)
            true
        } catch (e: Exception) {
            Log.e(TAG, "startInDefaultView", e)

            showLaunchBrowserError()
            false
        }
    }

    private fun showLoadProvidersError() {
        view?.also {
            Snackbar
                .make(it, R.string.chooseRemoteDictProvider_failedToLoadProvidersError, Snackbar.LENGTH_LONG)
                .showLifecycleAwareSnackbar(lifecycle)
        }
    }

    private fun showLaunchBrowserError() {
        view?.let {
            Snackbar
                .make(it, R.string.chooseRemoteDictProvider_failedToLaunchBrowser, Snackbar.LENGTH_LONG)
                .showLifecycleAwareSnackbar(lifecycle)
        }
    }

    companion object {
        private const val TAG = "ChooseRDPFragment"

        private const val SAVED_STATE_IS_BROWSER_LAUNCHED =
            "io.github.pelmenstar1.digiDict.ChooseRDPFragment.isBrowserLaunched"

        internal fun RemoteDictionaryProviderInfo.resolvedUrlParsed(query: String): Uri {
            return Uri.parse(resolvedUrl(query))
        }
    }
}