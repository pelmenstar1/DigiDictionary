package io.github.pelmenstar1.digiDict.utils

import android.content.Context
import android.os.Build
import java.util.*

fun interface LocaleProvider {
    fun get(): Locale

    companion object {
        fun fromContext(context: Context) = LocaleProvider { context.getLocaleCompat() }
        fun fromValue(value: Locale) = LocaleProvider { value }
    }
}

@Suppress("DEPRECATION")
fun Context.getLocaleCompat(): Locale {
    val config = resources.configuration

    return if (Build.VERSION.SDK_INT >= 24) {
        config.locales[0]
    } else {
        config.locale
    }
}