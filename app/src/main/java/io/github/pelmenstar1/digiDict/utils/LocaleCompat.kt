package io.github.pelmenstar1.digiDict.utils

import android.content.Context
import android.content.res.Configuration
import android.content.res.Resources
import android.os.Build
import java.util.*

@Suppress("DEPRECATION")
fun Context.getLocaleCompat(): Locale {
    val config = resources.configuration

    return if(Build.VERSION.SDK_INT >= 23) {
        config.locales[0]
    } else {
        config.locale
    }
}