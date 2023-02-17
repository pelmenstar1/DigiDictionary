package io.github.pelmenstar1.digiDict.common.android

import android.content.Context
import io.github.pelmenstar1.digiDict.common.getEnumFieldCount

abstract class EnumResourcesStringFormatter<T : Enum<T>>(
    context: Context,
    valueClass: Class<T>
) : ResourcesStringFormatter<T>(context, getEnumFieldCount(valueClass)) {
    final override fun getValueOrdinal(value: T): Int = value.ordinal
}