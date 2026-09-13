package io.github.pelmenstar1.digiDict

import android.app.Application
import dagger.hilt.android.HiltAndroidApp
import io.sentry.android.core.SentryAndroid
import io.sentry.android.fragment.FragmentLifecycleIntegration

@HiltAndroidApp
class DigiDictApplication : Application() {
    override fun onCreate() {
        super.onCreate()

        initSentry()
    }

    private fun initSentry() {
        val dsn = BuildConfig.SENTRY_DSN
        if (dsn.isEmpty()) {
            return
        }

        SentryAndroid.init(this) { options ->
            options.dsn = dsn
            options.environment = BuildConfig.BUILD_TYPE
            options.release =
                "${BuildConfig.APPLICATION_ID}@${BuildConfig.VERSION_NAME}+${BuildConfig.VERSION_CODE}"

            options.addIntegration(
                FragmentLifecycleIntegration(
                    this@DigiDictApplication,
                    enableFragmentLifecycleBreadcrumbs = true,
                    enableAutoFragmentLifecycleTracing = true
                )
            )

            options.tracesSampleRate = 1.0
            options.isDebug = BuildConfig.DEBUG
        }
    }
}
