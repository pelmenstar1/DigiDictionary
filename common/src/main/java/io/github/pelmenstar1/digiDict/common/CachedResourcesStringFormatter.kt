package io.github.pelmenstar1.digiDict.common

import android.content.res.Resources
import androidx.annotation.StringRes
import java.util.concurrent.atomic.AtomicReferenceArray

abstract class CachedResourcesStringFormatter<T>(
    private val resources: Resources,
    valueCount: Int
) : StringFormatter<T> {
    private val cache = AtomicReferenceArray<String>(valueCount)

    final override fun format(value: T): String {
        val ordinal = getValueOrdinal(value)
        val cachedStr = cache[ordinal]

        if (cachedStr != null) {
            return cachedStr
        }

        val resId = getResourceId(value)

        return resources.getString(resId).also {
            cache[ordinal] = it
        }
    }

    protected abstract fun getValueOrdinal(value: T): Int

    @StringRes
    protected abstract fun getResourceId(value: T): Int
}