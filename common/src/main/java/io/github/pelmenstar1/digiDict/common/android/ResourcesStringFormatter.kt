package io.github.pelmenstar1.digiDict.common.android

import android.content.Context
import androidx.annotation.StringRes
import io.github.pelmenstar1.digiDict.common.StringFormatter
import java.util.concurrent.atomic.AtomicReferenceArray

abstract class ResourcesStringFormatter<T>(context: Context, valueCount: Int) : StringFormatter<T> {
    private val cache = AtomicReferenceArray<String>(valueCount)
    private val resources = context.resources

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