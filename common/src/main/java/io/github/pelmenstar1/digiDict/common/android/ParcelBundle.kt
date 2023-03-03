package io.github.pelmenstar1.digiDict.common.android

import android.os.Build
import android.os.Bundle
import android.os.Parcel
import android.os.Parcelable

fun Parcel.readStringOrThrow() = requireNotNull(readString())
fun Bundle.getByteArrayOrThrow(key: String) = requireNotNull(getByteArray(key))

inline fun <reified T : Parcelable> Bundle.getParcelableCompat(key: String): T? {
    return getParcelableCompat(key, T::class.java)
}

fun <T : Parcelable> Bundle.getParcelableCompat(key: String, c: Class<T>): T? {
    return if (Build.VERSION.SDK_INT >= 33) {
        getParcelable(key, c)
    } else {
        @Suppress("DEPRECATION")
        getParcelable(key)
    }
}