package io.github.pelmenstar1.digiDict.common.android

import android.content.Context
import android.os.Build
import java.util.*

@Suppress("DEPRECATION")
fun Context.getLocaleCompat(): Locale {
    val config = resources.configuration

    return if (Build.VERSION.SDK_INT >= 24) {
        config.locales[0]
    } else {
        config.locale
    }
}