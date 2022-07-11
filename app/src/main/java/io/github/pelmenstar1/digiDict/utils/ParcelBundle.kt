package io.github.pelmenstar1.digiDict.utils

import android.content.Intent
import android.os.Bundle
import android.os.Parcel
import android.os.Parcelable

fun Parcel.readStringOrThrow() = requireNotNull(readString())

fun Bundle.getStringOrThrow(key: String) = requireNotNull(getString(key))
fun <T : Parcelable> Bundle.getParcelableOrThrow(key: String): T = requireNotNull(getParcelable(key))
fun Intent.getIntArrayExtraOrThrow(key: String) = requireNotNull(getIntArrayExtra(key))