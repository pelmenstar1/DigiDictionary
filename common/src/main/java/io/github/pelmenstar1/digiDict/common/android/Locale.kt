package io.github.pelmenstar1.digiDict.common.android

import android.content.Context
import java.util.Locale

fun Context.getLocaleCompat(): Locale = resources.configuration.locales[0]
