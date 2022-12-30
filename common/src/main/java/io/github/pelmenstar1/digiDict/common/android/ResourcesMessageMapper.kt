package io.github.pelmenstar1.digiDict.common.android

import android.content.Context
import androidx.annotation.StringRes
import io.github.pelmenstar1.digiDict.common.MessageMapper
import io.github.pelmenstar1.digiDict.common.getEnumFieldCount
import java.util.concurrent.atomic.AtomicReferenceArray

abstract class ResourcesMessageMapper<T : Enum<T>>(
    context: Context,
    enumCount: Int
) : MessageMapper<T> {
    private val res = context.resources
    private val cachedStrings = AtomicReferenceArray<String?>(enumCount)

    final override fun map(type: T): String {
        // Even if two threads are simultaneously trying to map same type T, then state remains consistent anyway,
        // simply Resources.getString() will be called twice.
        val cachedStr = cachedStrings.get(type.ordinal)
        if (cachedStr != null) {
            return cachedStr
        }

        val id = mapToStringResource(type)

        return res.getString(id).also {
            cachedStrings.set(type.ordinal, it)
        }
    }

    @StringRes
    protected abstract fun mapToStringResource(type: T): Int
}

inline fun <reified T : Enum<T>> resourcesMessageMapper(
    context: Context,
    crossinline mapToStringRes: (T) -> Int
): ResourcesMessageMapper<T> {
    return object : ResourcesMessageMapper<T>(context, getEnumFieldCount<T>()) {
        override fun mapToStringResource(type: T) = mapToStringRes(type)
    }
}